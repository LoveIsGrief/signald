package io.finn.signald.handlers.grpc;

import io.grpc.stub.StreamObserver;

public class SignalDaemonImpl extends SignalDaemonGrpc.SignalDaemonImplBase {
  @Override
  public void register(RegisterMessage request, StreamObserver<SuccessResponse> responseObserver) {
    RegisterHandler.handle(request, responseObserver);
  }
}
