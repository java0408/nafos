package nafos.bootStrap.handle.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;
import nafos.bootStrap.handle.currency.AsyncSessionHandle;
import nafos.core.Thread.Processors;
import nafos.bootStrap.handle.ExecutorPoolChoose;
import nafos.core.entry.AsyncTaskMode;
import nafos.core.entry.HttpRouteClassAndMethod;
import nafos.core.entry.http.NafosRequest;
import nafos.core.helper.SpringApplicationContextHolder;
import nafos.core.mode.InitMothods;
import nafos.core.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * @Author 黄新宇
 * @Date 2018/10/8 下午9:49
 * @Description TODO
 **/
@Service
public class HttpExecutorPoolChoose implements ExecutorPoolChoose {
    @Autowired
    HttpRouteHandle httpRouteHandle;

    @Override
    public void choosePool(ChannelHandlerContext ctx, Object msg) {
        NsRequest request = (NsRequest) msg;

        // 根据URI来查找object.method.invoke
        String uri = request.method() + ":" + parseUri(request.uri());

        if ("GET:/favicon.ico".equals(uri)) return;

        HttpRouteClassAndMethod httpRouteClassAndMethod = InitMothods.getHttpHandler(uri);

        // 1.寻找路由失败
        if (httpRouteClassAndMethod == null) {
            NettyUtil.sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        boolean isRunOnWork = httpRouteClassAndMethod.isRunOnWorkGroup();

        String cookieId = new NafosRequest(request).getNafosCookieId();
        cookieId = ObjectUtil.isNotNull(cookieId) ? cookieId : CastUtil.castString(new Random().nextInt(10));
        int queuecCode = cookieId.hashCode() % Processors.getProcess();

        if (!isRunOnWork) {
            ReferenceCountUtil.retain(request);
            AsyncSessionHandle.runTask(queuecCode, new AsyncTaskMode(ctx, request, httpRouteClassAndMethod));
            return;
        }

        ReferenceCountUtil.retain(request);
        SpringApplicationContextHolder.getSpringBeanForClass(HttpRouteHandle.class)
                .route(ctx, request, httpRouteClassAndMethod);


    }

    /**
     * 截取uri
     *
     * @param uri
     * @return
     */
    private String parseUri(String uri) {
        // 1)null
        if (null == uri) {
            return null;
        }
        // 2)截断?
        int index = uri.indexOf("?");
        if (-1 != index) {
            uri = uri.substring(0, index);
        }
        return uri;
    }
}