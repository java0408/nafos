package com;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import nafos.core.annotation.controller.Handle;
import nafos.core.mode.RouteFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @Author 黄新宇
 * @Date 2018/10/11 下午4:25
 * @Description TODO
 **/
@nafos.core.annotation.controller.Controller
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    @Handle(uri="/hi",method = "GET",type="JSON",runOnWorkGroup = false)
    public Object test(Map map){

        logger.info(map.toString());
        return map;
    }


    @Handle(code = 1000,type="JSON")
    public void h(Channel channel, Map map,byte[] id){

        logger.info(map.toString());
//        channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer("121".getBytes())));
        channel.writeAndFlush("121".getBytes());
    }
}