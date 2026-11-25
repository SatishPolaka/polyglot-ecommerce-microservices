/*
 * Copyright 2018, Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hipstershop;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import hipstershop.Demo.Ad;
import hipstershop.Demo.AdRequest;
import hipstershop.Demo.AdResponse;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.services.*;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AdService {

  private static final Logger logger = LogManager.getLogger(AdService.class);

  @SuppressWarnings("FieldCanBeLocal")
  private static int MAX_ADS_TO_SERVE = 2;

  private Server server;
  private HealthStatusManager healthMgr;

  private static final AdService service = new AdService();

  private void start() throws IOException {
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "9555"));
    healthMgr = new HealthStatusManager();

    server =
        ServerBuilder.forPort(port)
            .addService(new AdServiceImpl())
            .addService(healthMgr.getHealthService())
            .build()
            .start();
    logger.info("Ad Service started, listening on " + port);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                  System.err.println(
                      "*** shutting down gRPC ads server since JVM is shutting down");
                  AdService.this.stop();
                  System.err.println("*** server shut down");
                }));
    healthMgr.setStatus("", ServingStatus.SERVING);
  }

  private void stop() {
    if (server != null) {
      healthMgr.clearStatus("");
      server.shutdown();
    }
  }

  private static class AdServiceImpl extends hipstershop.AdServiceGrpc.AdServiceImplBase {

    /**
     * Retrieves ads based on context provided in the request {@code AdRequest}.
     *
     * @param req the request containing context.
     * @param responseObserver the stream observer which gets notified with the value of {@code
     *     AdResponse}
     */
    @Override
    public void getAds(AdRequest req, StreamObserver<AdResponse> responseObserver) {
      AdService service = AdService.getInstance();
      try {
        List<Ad> allAds = new ArrayList<>();
        logger.info("received ad request (context_words=" + req.getContextKeysList() + ")");
        if (req.getContextKeysCount() > 0) {
          for (int i = 0; i < req.getContextKeysCount(); i++) {
            Collection<Ad> ads = service.getAdsByCategory(req.getContextKeys(i));
            allAds.addAll(ads);
          }
        } else {
          allAds = service.getRandomAds();
        }
        if (allAds.isEmpty()) {
          // Serve random ads.
          allAds = service.getRandomAds();
        }
        AdResponse reply = AdResponse.newBuilder().addAllAds(allAds).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
      } catch (StatusRuntimeException e) {
        logger.log(Level.WARN, "GetAds Failed with status {}", e.getStatus());
        responseObserver.onError(e);
      }
    }
  }

  private static final ImmutableListMultimap<String, Ad> adsMap = createAdsMap();

  private Collection<Ad> getAdsByCategory(String category) {
    return adsMap.get(category);
  }

  private static final Random random = new Random();

  private List<Ad> getRandomAds() {
    List<Ad> ads = new ArrayList<>(MAX_ADS_TO_SERVE);
    Collection<Ad> allAds = adsMap.values();
    for (int i = 0; i < MAX_ADS_TO_SERVE; i++) {
      ads.add(Iterables.get(allAds, random.nextInt(allAds.size())));
    }
    return ads;
  }

  private static AdService getInstance() {
    return service;
  }

  /** Await termination on the main thread since the grpc library uses daemon threads. */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  private static ImmutableListMultimap<String, Ad> createAdsMap() {
    Ad hairdryer =
        Ad.newBuilder()
            .setRedirectUrl("/product/2ZYFJ3GM2N")
            .setText("Hairdryer, 50% off. Three speeds, one of them called I have seven minutes.")
            .build();
    Ad tankTop =
        Ad.newBuilder()
            .setRedirectUrl("/product/66VCHSJNUP")
            .setText("Tank top, 20% off. Looks like you already stretched.")
            .build();
    Ad hoodie =
        Ad.newBuilder()
            .setRedirectUrl("/product/H00D13MEET")
            .setText("Emergency Meeting Hoodie. 25% off looking like you meant to be on camera.")
            .build();
    Ad candleHolder =
        Ad.newBuilder()
            .setRedirectUrl("/product/0PUK6V6EV0")
            .setText("Candle holder, 30% off. For dramatic lighting and one personality trait.")
            .build();
    Ad bambooGlassJar =
        Ad.newBuilder()
            .setRedirectUrl("/product/9SIQT8TOJO")
            .setText("Bamboo jar, 10% off. Perfect for snacks you swore were for guests.")
            .build();
    Ad watch =
        Ad.newBuilder()
            .setRedirectUrl("/product/1YMWWN1N4O")
            .setText("Watch sale. It tells time and how long this meeting has been going.")
            .build();
    Ad mug =
        Ad.newBuilder()
            .setRedirectUrl("/product/6E92ZMYYFZ")
            .setText("Mustard mug. Buy two, get a third for the refill that became the plan.")
            .build();
    Ad loafers =
        Ad.newBuilder()
            .setRedirectUrl("/product/L9ECAV7KIM")
            .setText("Loafers. Buy one, get the other one, because that is how shoes work.")
            .build();
    Ad lamp =
        Ad.newBuilder()
            .setRedirectUrl("/product/LAMPGREG01")
            .setText("Meet Greg, the desk lamp. Warm light. No recurring meetings.")
            .build();
    Ad tote =
        Ad.newBuilder()
            .setRedirectUrl("/product/TOTE4DOCS1")
            .setText("I Read the Docs tote. We will not check. Your laptop will look faster.")
            .build();
    Ad ceramicBowl =
        Ad.newBuilder()
            .setRedirectUrl("/product/CERAMICBWL1")
            .setText("Handthrown Ceramic Bowl. Each one slightly different, like your morning mood. 15% off.")
            .build();
    Ad leatherWallet =
        Ad.newBuilder()
            .setRedirectUrl("/product/LEATHRWLT1")
            .setText("Slim Leather Wallet. Full-grain leather. Gets better with age. Unlike most things.")
            .build();
    Ad deskMat =
        Ad.newBuilder()
            .setRedirectUrl("/product/DESKMATPRO1")
            .setText("Extended Desk Mat. Unify your setup. Non-slip base. Very slip-resistant opinions.")
            .build();
    Ad linenShirt =
        Ad.newBuilder()
            .setRedirectUrl("/product/LINENSHIRT1")
            .setText("Relaxed Linen Shirt. Wrinkles on purpose. 20% off this week.")
            .build();
    Ad frenchPress =
        Ad.newBuilder()
            .setRedirectUrl("/product/FRENCHPRS1")
            .setText("French Press. Four minutes to a better morning. Tastes like you planned it.")
            .build();
    Ad chunkyKnit =
        Ad.newBuilder()
            .setRedirectUrl("/product/CHUNKYKNIT1")
            .setText("Chunky Knit Sweater. Trending now. Warm enough to regret leaving the house.")
            .build();
    Ad aromaDiffuser =
        Ad.newBuilder()
            .setRedirectUrl("/product/AROMADIFF1")
            .setText("Ceramic Aroma Diffuser. Fills the room with calm. Runs 6 hours. Judgment-free.")
            .build();
    Ad archMirror =
        Ad.newBuilder()
            .setRedirectUrl("/product/MIRRORWALL1")
            .setText("Arch Wall Mirror. New arrival. Makes the room look bigger and your outfit more considered.")
            .build();
    Ad usbCharger =
        Ad.newBuilder()
            .setRedirectUrl("/product/CHARGEPD01")
            .setText("65W USB-C GaN Charger. Two ports. One outlet. Zero excuses.")
            .build();
    Ad crossbodyBag =
        Ad.newBuilder()
            .setRedirectUrl("/product/CROSSBODY1")
            .setText("Mini Crossbody Bag. Pebbled leather. Room for your phone, keys, and one decision.")
            .build();
    Ad waxedDuffle =
        Ad.newBuilder()
            .setRedirectUrl("/product/DRYRBAG001")
            .setText("Waxed Canvas Duffle. 40L. Water-resistant. A weekend's worth of optimism.")
            .build();
    return ImmutableListMultimap.<String, Ad>builder()
        .putAll("clothing", tankTop, hoodie, linenShirt, chunkyKnit)
        .putAll("accessories", watch, tote, leatherWallet)
        .putAll("footwear", loafers)
        .putAll("hair", hairdryer)
        .putAll("decor", candleHolder, lamp, aromaDiffuser, archMirror)
        .putAll("kitchen", bambooGlassJar, mug, ceramicBowl, frenchPress)
        .putAll("office", lamp, deskMat, usbCharger)
        .putAll("home", lamp, candleHolder, aromaDiffuser, archMirror)
        .putAll("bags", tote, crossbodyBag, waxedDuffle)
        .putAll("tech", watch, usbCharger)
        .build();
  }

  private static void initStats() {
    if (System.getenv("DISABLE_STATS") != null) {
      logger.info("Stats disabled.");
      return;
    }
    logger.info("Stats enabled, but temporarily unavailable");

    long sleepTime = 10; /* seconds */
    int maxAttempts = 5;

    // TODO(arbrown) Implement OpenTelemetry stats

  }

  private static void initTracing() {
    if (System.getenv("DISABLE_TRACING") != null) {
      logger.info("Tracing disabled.");
      return;
    }
    logger.info("Tracing enabled but temporarily unavailable");
    logger.info("See https://github.com/GoogleCloudPlatform/microservices-demo/issues/422 for more info.");

    // TODO(arbrown) Implement OpenTelemetry tracing
    
    logger.info("Tracing enabled - Stackdriver exporter initialized.");
  }

  /** Main launches the server from the command line. */
  public static void main(String[] args) throws IOException, InterruptedException {

    new Thread(
            () -> {
              initStats();
              initTracing();
            })
        .start();

    // Start the RPC server. You shouldn't see any output from gRPC before this.
    logger.info("AdService starting.");
    final AdService service = AdService.getInstance();
    service.start();
    service.blockUntilShutdown();
  }
}
