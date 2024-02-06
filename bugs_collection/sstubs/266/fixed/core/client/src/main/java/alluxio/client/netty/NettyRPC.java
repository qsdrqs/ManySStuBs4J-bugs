/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.client.netty;

import alluxio.network.protocol.RPCProtoMessage;
import alluxio.util.CommonUtils;
import alluxio.util.proto.ProtoMessage;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Netty blocking RPC client. This provides a simple way to send a request and wait for response
 * via netty. The user needs to make sure that the request is properly handled on the server.
 */
public final class NettyRPC {
  private NettyRPC() {}  // prevent instantiation

  /**
   * Sends a request and waits for a response.
   *
   * @param context the netty RPC context
   * @param request the RPC request
   * @return the RPC response
   */
  public static ProtoMessage call(final NettyRPCContext context, ProtoMessage request) {
    Channel channel = Preconditions.checkNotNull(context.getChannel());
    Promise<ProtoMessage> promise = channel.eventLoop().newPromise();
    channel.pipeline().addLast(new RPCHandler(promise));
    channel.writeAndFlush(new RPCProtoMessage(request));
    try {
      return promise.get(context.getTimeoutMs(), TimeUnit.MILLISECONDS);
    } catch (ExecutionException e) {
      throw CommonUtils.propagate(e.getCause() == null ? e : e.getCause());
    } catch (TimeoutException e) {
      throw CommonUtils.propagate(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      channel.pipeline().removeLast();
    }
  }

  /**
   * Sends a request and waits for a response. This can only be used if the response type is
   * {@link alluxio.proto.dataserver.Protocol.Response}.
   *
   * @param context the netty RPC context
   * @param request the RPC request
   */
  public static void callDefaultResponse(final NettyRPCContext context, ProtoMessage request) {
    ProtoMessage message = call(context, request);
    Preconditions.checkState(message.isResponse());
    CommonUtils.unwrapResponse(message.asResponse());
  }

  /**
   * Netty RPC client handler.
   */
  private static class RPCHandler extends ChannelInboundHandlerAdapter {
    /** The promise to wait for the response. */
    private final Promise<ProtoMessage> mPromise;

    /**
     * Creates an instance of {@link RPCHandler}.
     *
     * @param promise the promise
     */
    public RPCHandler(Promise<ProtoMessage> promise) {
      mPromise = promise;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (!acceptMessage(msg)) {
        ctx.fireChannelRead(msg);
        return;
      }

      ProtoMessage message = ((RPCProtoMessage) msg).getMessage();
      mPromise.trySuccess(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      mPromise.tryFailure(cause);
      ctx.close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
      mPromise.tryFailure(new IOException("ChannelClosed"));
      ctx.fireChannelUnregistered();
    }

    /**
     * @param msg the messsage
     * @return true if the message should be accepted as a response
     */
    protected boolean acceptMessage(Object msg) {
      return msg instanceof RPCProtoMessage;
    }
  }
}