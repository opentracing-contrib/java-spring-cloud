/**
 * Copyright 2017-2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.cloud.starter.jaeger;

import com.uber.jaeger.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("opentracing.jaeger")
public class JaegerConfigurationProperties {

  private final RemoteReporter remoteReporter = new RemoteReporter();
  private final HttpSender httpSender = new HttpSender();
  private final UdpSender udpSender = new UdpSender();
  private final ConstSampler constSampler = new ConstSampler();
  private final ProbabilisticSampler probabilisticSampler = new ProbabilisticSampler();
  private final RateLimitingSampler rateLimitingSampler = new RateLimitingSampler();
  private final RemoteControlledSampler remoteControlledSampler = new RemoteControlledSampler();
  /**
   * Enable Jaeger Tracer
   */
  private boolean enabled = true;

  /**
   * Whether spans should be logged to the console
   */
  private boolean logSpans = false;
  /**
   * Enable the handling of B3 headers like "X-B3-TraceId" This setting should be used when it is
   * desired for Jaeger to be able to join traces started by other Zipkin instrumented applications
   */
  private boolean enableB3Propagation = false;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isLogSpans() {
    return logSpans;
  }

  public void setLogSpans(boolean logSpans) {
    this.logSpans = logSpans;
  }

  public boolean isEnableB3Propagation() {
    return enableB3Propagation;
  }

  public void setEnableB3Propagation(boolean enableB3Propagation) {
    this.enableB3Propagation = enableB3Propagation;
  }

  public HttpSender getHttpSender() {
    return httpSender;
  }

  public RemoteReporter getRemoteReporter() {
    return remoteReporter;
  }

  public UdpSender getUdpSender() {
    return udpSender;
  }

  public ConstSampler getConstSampler() {
    return constSampler;
  }

  public ProbabilisticSampler getProbabilisticSampler() {
    return probabilisticSampler;
  }

  public RateLimitingSampler getRateLimitingSampler() {
    return rateLimitingSampler;
  }

  public RemoteControlledSampler getRemoteControlledSampler() {
    return remoteControlledSampler;
  }


  public static class RemoteReporter {

    private Integer flushInterval;

    private Integer maxQueueSize;

    public Integer getFlushInterval() {
      return flushInterval;
    }

    public void setFlushInterval(Integer flushInterval) {
      this.flushInterval = flushInterval;
    }

    public Integer getMaxQueueSize() {
      return maxQueueSize;
    }

    public void setMaxQueueSize(Integer maxQueueSize) {
      this.maxQueueSize = maxQueueSize;
    }
  }

  /**
   * If the URL is set, then HttpSender is used regardless
   * of the configuration in UdpSender
   */
  public static class HttpSender {

    private String url;

    private Integer maxPayload = 0;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public Integer getMaxPayload() {
      return maxPayload;
    }

    public void setMaxPayload(Integer maxPayload) {
      this.maxPayload = maxPayload;
    }
  }

  public static class UdpSender {

    private String host = "localhost";

    private int port = 6831;

    private int maxPacketSize = 0;

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public int getMaxPacketSize() {
      return maxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
      this.maxPacketSize = maxPacketSize;
    }
  }

  public static class ConstSampler {

    private Boolean decision;

    public Boolean getDecision() {
      return decision;
    }

    public void setDecision(Boolean decision) {
      this.decision = decision;
    }
  }

  public static class ProbabilisticSampler {

    private Double samplingRate;

    public Double getSamplingRate() {
      return samplingRate;
    }

    public void setSamplingRate(Double samplingRate) {
      this.samplingRate = samplingRate;
    }
  }

  public static class RateLimitingSampler {

    private Double maxTracesPerSecond;

    public Double getMaxTracesPerSecond() {
      return maxTracesPerSecond;
    }

    public void setMaxTracesPerSecond(Double maxTracesPerSecond) {
      this.maxTracesPerSecond = maxTracesPerSecond;
    }
  }

  public static class RemoteControlledSampler {

    /**
     * i.e. localhost:5778
     */
    private String hostPort;

    private Double samplingRate = Configuration.DEFAULT_SAMPLING_PROBABILITY;


    public String getHostPort() {
      return hostPort;
    }

    public void setHostPort(String hostPort) {
      this.hostPort = hostPort;
    }

    public Double getSamplingRate() {
      return samplingRate;
    }

    public void setSamplingRate(Double samplingRate) {
      this.samplingRate = samplingRate;
    }
  }
}
