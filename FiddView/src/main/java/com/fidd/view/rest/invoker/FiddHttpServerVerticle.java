package com.fidd.view.rest.invoker;

import com.fidd.service.FiddContentServiceManager;
import com.fidd.view.rest.controller.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FiddHttpServerVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(FiddHttpServerVerticle.class);
    private final String specFile;

    private final MessagesApiHandler messagesHandler;
    private final int fiddApiServerPort;

    protected final FiddContentServiceManager fiddContentServiceManager;

    public FiddHttpServerVerticle(String specFile, FiddContentServiceManager fiddContentServiceManager, int fiddApiServerPort) {
        this.specFile = specFile;
        this.fiddContentServiceManager = fiddContentServiceManager;
        this.fiddApiServerPort = fiddApiServerPort;

        messagesHandler = new MessagesApiHandler(new MessagesApiCustomImpl(fiddContentServiceManager));
    }

    @Override
    public void start(Promise<Void> startPromise) {
        RouterBuilder.create(vertx, specFile)
            .map(builder -> {
              builder.setOptions(new RouterBuilderOptions()
                  // TODO: consider implementing configuration for this
                  // For production use case, you need to enable this flag and provide the proper security handler
                  .setRequireSecurityHandlers(false)
              );

              messagesHandler.mount(builder);

              Router router = builder.createRouter();
              router.errorHandler(400, this::validationFailureHandler);

              return router;
            })
            .compose(router ->
                vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(fiddApiServerPort)
            )
            .onSuccess(server -> logger.info("Http verticle deploy successful"))
            .onFailure(t -> logger.error("Http verticle failed to deploy", t))
            // Complete the start promise
            .<Void>mapEmpty().onComplete(startPromise);
    }

    private void validationFailureHandler(RoutingContext rc) {
         rc.response().setStatusCode(400)
                 .end("Bad Request : " + rc.failure().getMessage());
    }
}
