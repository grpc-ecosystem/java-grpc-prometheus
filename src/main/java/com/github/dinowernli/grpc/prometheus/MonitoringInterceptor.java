// Copyright 2016 Dino Wernli
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.github.dinowernli.grpc.prometheus;

import java.time.Clock;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/** An interceptor which send stats about the rpcs to Prometheus. */
public class MonitoringInterceptor implements ServerInterceptor {
  private final Clock clock;

  public MonitoringInterceptor(Clock clock) {
    this.clock = clock;
  }

  @Override
  public <R, S> ServerCall.Listener<R> interceptCall(
      MethodDescriptor<R, S> method,
      ServerCall<S> call,
      Metadata requestHeaders,
      ServerCallHandler<R, S> next) {
    return next.startCall(
        method, MonitoringForwardingServerCall.create(call, clock, method), requestHeaders);
  }
}
