/**
 *
 * Copyright (C) 2009 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */
package org.jclouds.http.httpnio.pool;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.UnmappableCharacterException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpException;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.nio.SSLClientIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.HttpParams;
import org.jclouds.Constants;
import org.jclouds.http.HttpCommandRendezvous;
import org.jclouds.http.TransformingHttpCommand;
import org.jclouds.http.pool.HttpCommandConnectionHandle;
import org.jclouds.http.pool.HttpCommandConnectionPool;

import com.google.common.annotations.VisibleForTesting;

/**
 * Connection Pool for HTTP requests that utilizes Apache HTTPNio
 * 
 * @author Adrian Cole
 */
public class NioHttpCommandConnectionPool extends HttpCommandConnectionPool<NHttpConnection>
         implements EventListener {

   @Override
   public String toString() {
      return "NioHttpCommandConnectionPool [ target=" + target + ", endPoint=" + getEndPoint()
               + ", hashCode=" + hashCode() + " ]";
   }

   private final NHttpClientConnectionPoolSessionRequestCallback sessionCallback;
   private final DefaultConnectingIOReactor ioReactor;
   private final IOEventDispatch dispatch;
   private final InetSocketAddress target;

   public static interface Factory extends HttpCommandConnectionPool.Factory<NHttpConnection> {
      NioHttpCommandConnectionPool create(URI endPoint);
   }

   @Inject
   public NioHttpCommandConnectionPool(ExecutorService executor, Semaphore allConnections,
            BlockingQueue<HttpCommandRendezvous<?>> commandQueue,
            BlockingQueue<NHttpConnection> available, AsyncNHttpClientHandler clientHandler,
            DefaultConnectingIOReactor ioReactor, HttpParams params, URI endPoint,
            @Named(Constants.PROPERTY_MAX_CONNECTION_REUSE) int maxConnectionReuse,
            @Named(Constants.PROPERTY_MAX_SESSION_FAILURES) int maxSessionFailures) {
      super(executor, allConnections, commandQueue, available, endPoint, maxConnectionReuse,
               maxSessionFailures);
      String host = checkNotNull(checkNotNull(endPoint, "endPoint").getHost(), String.format(
               "Host null for endpoint %s", endPoint));
      int port = endPoint.getPort();
      if (endPoint.getScheme().equals("https")) {
         try {
            this.dispatch = provideSSLClientEventDispatch(clientHandler, params);
         } catch (KeyManagementException e) {
            throw new RuntimeException("SSL error creating a connection to " + endPoint, e);
         } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SSL error creating a connection to " + endPoint, e);
         }
         if (port == -1)
            port = 443;
      } else {
         this.dispatch = provideClientEventDispatch(clientHandler, params);
         if (port == -1)
            port = 80;
      }
      checkArgument(port > 0, String.format("Port %d not in range for endpoint %s", endPoint
               .getPort(), endPoint));
      this.ioReactor = ioReactor;
      this.sessionCallback = new NHttpClientConnectionPoolSessionRequestCallback();
      this.target = new InetSocketAddress(host, port);
      clientHandler.setEventListener(this);
   }

   public static IOEventDispatch provideSSLClientEventDispatch(AsyncNHttpClientHandler handler,
            HttpParams params) throws NoSuchAlgorithmException, KeyManagementException {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, null, null);
      return new SSLClientIOEventDispatch(handler, context, params);
   }

   public static IOEventDispatch provideClientEventDispatch(AsyncNHttpClientHandler handler,
            HttpParams params) {
      return new DefaultClientIOEventDispatch(handler, params);
   }

   @Override
   public void start() {
      synchronized (this.statusLock) {
         if (this.status.compareTo(Status.INACTIVE) == 0) {
            executorService.execute(new Runnable() {
               public void run() {
                  try {
                     ioReactor.execute(dispatch);
                  } catch (IOException e) {
                     exception.set(e);
                     logger.error(e, "Error dispatching %1$s", dispatch);
                     status = Status.SHUTDOWN_REQUEST;
                  }
               }
            });
         }
         super.start();
      }
   }

   public void shutdownReactor(long waitMs) {
      try {
         this.ioReactor.shutdown(waitMs);
      } catch (IOException e) {
         logger.error(e, "Error shutting down reactor");
      }
   }

   @Override
   public boolean connectionValid(NHttpConnection conn) {
      boolean isOpen = conn.isOpen();
      boolean isStale = conn.isStale();
      long requestCount = conn.getMetrics().getRequestCount();
      return isOpen && !isStale && requestCount < maxConnectionReuse;
   }

   @Override
   public void shutdownConnection(NHttpConnection conn) {
      if (conn.getStatus() == NHttpConnection.ACTIVE) {
         try {
            conn.shutdown();
         } catch (IOException e) {
            logger.error(e, "Error shutting down connection");
         }
      }
   }

   @Override
   protected void doWork() throws Exception {
      createNewConnection();
   }

   @Override
   protected void doShutdown() {
      // Give the I/O reactor 1 sec to shut down
      shutdownReactor(1000);
      assert this.ioReactor.getStatus().equals(IOReactorStatus.SHUT_DOWN) : "incorrect status after io reactor shutdown :"
               + this.ioReactor.getStatus();
   }

   @Override
   protected void createNewConnection() throws InterruptedException {
      boolean acquired = allConnections.tryAcquire(1, TimeUnit.SECONDS);
      if (acquired) {
         if (shouldDoWork()) {
            logger.trace("Opening: %s", getTarget());
            ioReactor.connect(getTarget(), null, null, sessionCallback);
         } else {
            allConnections.release();
         }
      }
   }

   @Override
   protected void associateHandleWithConnection(
            HttpCommandConnectionHandle<NHttpConnection> handle, NHttpConnection connection) {
      connection.getContext().setAttribute("command-handle", handle);
   }

   @Override
   protected NioHttpCommandConnectionHandle getHandleFromConnection(NHttpConnection connection) {
      return (NioHttpCommandConnectionHandle) connection.getContext()
               .getAttribute("command-handle");
   }

   class NHttpClientConnectionPoolSessionRequestCallback implements SessionRequestCallback {

      /**
       * {@inheritDoc}
       */
      @Override
      public void completed(SessionRequest request) {

      }

      /**
       * @see releaseConnectionAndSetResponseException
       */
      @Override
      public void cancelled(SessionRequest request) {
         releaseConnectionAndSetResponseException(request, new CancellationException(
                  "Cancelled request: " + request.getRemoteAddress()));
      }

      /**
       * Releases a connection and associates the current exception with the request using the
       * session.
       */
      @VisibleForTesting
      void releaseConnectionAndSetResponseException(SessionRequest request, Exception e) {
         allConnections.release();
         TransformingHttpCommand<?> frequest = (TransformingHttpCommand<?>) request.getAttachment();
         if (frequest != null) {
            frequest.setException(e);
         }
      }

      /**
       * Disables the pool, if {@code maxSessionFailures} is reached}
       * 
       * @see releaseConnectionAndSetResponseException
       */
      @Override
      public void failed(SessionRequest request) {
         int count = currentSessionFailures.getAndIncrement();
         releaseConnectionAndSetResponseException(request, request.getException());
         if (count >= maxSessionFailures) {
            logger.error(request.getException(),
                     "Exceeded maximum Session failures: %d, Disabling pool for %s",
                     maxSessionFailures, getTarget());
            exception.set(request.getException());
         }

      }

      /**
       * @see releaseConnectionAndSetResponseException
       */
      @Override
      public void timeout(SessionRequest request) {
         releaseConnectionAndSetResponseException(request, new TimeoutException("Timeout on: "
                  + request.getRemoteAddress()));
      }

   }

   public void connectionOpen(NHttpConnection conn) {
      conn.setSocketTimeout(0);
      available.offer(conn);
      logger.trace("Opened: %s", getTarget());
   }

   public void connectionTimeout(NHttpConnection conn) {
      String message = String.format("Timeout on : %s - timeout %d", getTarget(), conn
               .getSocketTimeout());
      logger.warn(message);
      resubmitIfRequestIsReplayable(conn, new TimeoutException(message));
   }

   public void connectionClosed(NHttpConnection conn) {
      logger.trace("Closed: %s", getTarget());
   }

   public void fatalIOException(IOException ex, NHttpConnection conn) {
      logger.error(ex, "IO Exception: %s", getTarget());
      HttpCommandRendezvous<?> rendezvous = getCommandFromConnection(conn);
      if (rendezvous != null) {
         /**
          * these exceptions, while technically i/o are unresolvable. set the error on the command
          * itself so that it doesn't replay.
          */
         if (ex instanceof UnmappableCharacterException) {
            setExceptionOnCommand(ex, rendezvous);
         } else {
            resubmitIfRequestIsReplayable(conn, ex);
         }
      }
   }

   public void fatalProtocolException(HttpException ex, NHttpConnection conn) {
      logger.error(ex, "Protocol Exception: %s", getTarget());
      setExceptionOnCommand(conn, ex);
   }

   @Override
   protected NioHttpCommandConnectionHandle createHandle(HttpCommandRendezvous<?> command,
            NHttpConnection conn) {
      try {
         return new NioHttpCommandConnectionHandle(allConnections, available, endPoint, command,
                  conn);
      } catch (InterruptedException e) {
         throw new RuntimeException("Interrupted creating a handle to " + conn, e);
      }
   }

   @Override
   protected boolean isReplayable(HttpCommandRendezvous<?> rendezvous) {
      return rendezvous.getCommand().isReplayable();
   }

   @VisibleForTesting
   InetSocketAddress getTarget() {
      return target;
   }

}