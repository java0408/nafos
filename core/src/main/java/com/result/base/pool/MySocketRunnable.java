package com.result.base.pool;

import com.result.base.cache.Client;
import com.result.base.cache.NameSpace;
import com.result.base.config.ConfigForSystemMode;
import com.result.base.entry.Base.BaseSocketMessage;
import com.result.base.entry.ResultStatus;
import com.result.base.entry.RouteClassAndMethod;
import com.result.base.entry.SocketRouteClassAndMethod;
import com.result.base.enums.SocketBinaryType;
import com.result.base.handle.ZlibMessageHandle;
import com.result.base.inits.InitMothods;
import com.result.base.tools.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class MySocketRunnable implements Runnable {
    // logger
    private static final Logger logger = LoggerFactory.getLogger(MySocketRunnable.class);

    // 保存变量
    private ChannelHandlerContext context;
    private Object msg;
    private WebSocketServerHandshaker handshaker;


    public MySocketRunnable(ChannelHandlerContext ctx, Object msg) {
        ReferenceCountUtil.retain(msg);
        context = ctx;
        this.msg = msg;
    }


    @Override
    public void run() {
        Thread.currentThread().setName( "nettyGo-Thread");
        // 传统的HTTP接入
        if (msg instanceof FullHttpRequest) {
            try {
                handleHttpRequest(context, ((FullHttpRequest) msg));
            } catch (Exception e) {
                e.printStackTrace();
            }
            // WebSocket接入
        } else if (msg instanceof WebSocketFrame) {
            handlerWebSocketFrame(context, (WebSocketFrame) msg);
        }
    }

    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 1.判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            ctx.close();
            return;
        }
        // 2.判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content()));
            return;
        }
        // 3.如果是文本消息
        if (frame instanceof TextWebSocketFrame) {
            String msg = ((TextWebSocketFrame) frame).text();
            // 采用空格ping消息
            if (msg.equals(" ")) {
                return;
            }
            // 返回应答消息
            String clientUri = msg.substring(0, msg.indexOf("|"));
            String serverUri = msg.substring(msg.indexOf("|") + 1, msg.indexOf("|", msg.indexOf("|") + 1));
            routeMethod(serverUri, ctx.channel(), msg.substring(clientUri.length() + serverUri.length() + 2, msg.length()), clientUri);
            return;
        }

        // 4.如果是proto消息
        if (frame instanceof BinaryWebSocketFrame) {
            //获取压缩后的bytes
            byte[] contentBytes = new byte[frame.content().readableBytes()];
            frame.content().readBytes(contentBytes);
            contentBytes = ZlibMessageHandle.unZlibByteMessage(contentBytes);
            if (ConfigForSystemMode.BINARYTYPE.equals(SocketBinaryType.INTBEFORE.getType())) {
                byte[] idByte = new byte[4];//前端传过来的ID，原样返回
                System.arraycopy(contentBytes, 0, idByte, 0, 4);
                byte[] uriByte = new byte[4];//解析路由的uri
                System.arraycopy(contentBytes, 4, uriByte, 0, 4);
                int uri = ArrayUtil.byteArrayToInt(uriByte);
                byte[] contentByte = new byte[frame.content().readableBytes()];
                System.arraycopy(contentBytes, 8, uriByte, 0, contentBytes.length-8);
                routeMethod(ConfigForSystemMode.SOCKETROUTEMAP.get(uri), ctx.channel(), contentByte, idByte);
            } else if (ConfigForSystemMode.BINARYTYPE.equals(SocketBinaryType.PARENTFORBASESOCKETMESSAGE.getType())) {
                BaseSocketMessage baseSocketMessage = SerializationUtil.deserializeFromByte(contentBytes,
                        BaseSocketMessage.class);
                routeMethod(baseSocketMessage.getServerUri(), ctx.channel(), contentBytes, baseSocketMessage.getClientUri());
            } else {
                logger.error("================>>>>编码方式暂未开放，请使用parentForBaseSocketMessage或者intBefore");
            }
        }
    }

    /**
     * 获取到路由之后的处理方法
     *
     * @param
     */
    private void routeMethod(String uri, Channel channel, Object content, Object id) {
        ReferenceCountUtil.release(msg);
        SocketRouteClassAndMethod route = InitMothods.getTaskHandler(uri);
        if (ObjectUtil.isNull(route)) {
            logger.error("================>>>>路由方法未找到:" + uri);
            return;
        }
        if (content instanceof byte[]) {
            route.getMethod().invoke(SpringApplicationContextHolder.getSpringBeanForClass(route.getClazz()),
                    route.getIndex(), new Object[]{channel.attr(AttributeKey.valueOf("client")).get(), SerializationUtil.deserializeFromByte((byte[]) content, route.getParamType()), id});
        } else {
            route.getMethod().invoke(SpringApplicationContextHolder.getSpringBeanForClass(route.getClazz()),
                    route.getIndex(), new Object[]{channel.attr(AttributeKey.valueOf("client")).get(), JsonUtil.json2Object((String) content, route.getParamType()), id});
        }

    }


    /**
     * @return void
     * @Author 黄新宇
     * <p>
     * websocket握手方法
     */

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        // 如果HTTP解码失败，返回HHTP异常
        if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
            SendUtil.sendHttpResponse(ctx, req,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        //业务处理
        clientInitOnHandshake(ctx, req);
        // 构造握手响应返回，本机测试
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://" + req.headers().get(HttpHeaderNames.HOST) + req.uri(), null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            // 版本不支持
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
        ReferenceCountUtil.release(msg);
    }

    private void clientInitOnHandshake(ChannelHandlerContext ctx, FullHttpRequest req) {
        // 封装client
        ctx.channel().attr(AttributeKey.valueOf("client")).set(new Client(ctx.channel()));
        // 1.获取url后置参数
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
        Map<String, List<String>> parameters = queryStringDecoder.parameters();
        // 1.1)前置filter事件
        RouteClassAndMethod filter = InitMothods.getMessageFilter();
        if (ObjectUtil.isNotNull(filter)) {
            ResultStatus resultStatus = (ResultStatus) filter.getMethod().invoke(
                    SpringApplicationContextHolder.getSpringBeanForClass(filter.getClazz()), filter.getIndex(),
                    new Object[]{ctx, req});
            if (!resultStatus.isSuccess()) {
                NettyUtil.sendError(ctx, resultStatus.getResponseStatus());
                return;
            }
        }
        try {
            if (ObjectUtil.isNotNull(req.headers().get("token"))) {
                ctx.channel().attr(AttributeKey.valueOf("token")).set(AESUtil.decrypt(req.headers().get("token")));
             } else {
                ctx.channel().attr(AttributeKey.valueOf("token")).set(AESUtil.decrypt(parameters.get("token").get(0)));
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
        // 1.3设置命名空间
        ctx.channel().attr(AttributeKey.valueOf("nameSpace")).set(req.uri().substring(1, req.uri().indexOf("?")));
        NameSpace.inviteClient(req.uri().substring(1, req.uri().indexOf("?")),
                (Client) ctx.channel().attr(AttributeKey.valueOf("client")).get());
    }
}
