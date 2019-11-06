/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.xream.x7.reliable.api;

import java.util.concurrent.Callable;

/**
 * @author Sim 8966188@qq.com
 * @apiNote to use @EnableReliableManagement, @ReliableProducer and @ReliableOnConsumed, <br>
 *     has to implements ReliableBackend, develop and deploy 'Reliable Message Center'
 */
public interface ReliableBackend {

    /**
     * @param isTcc
     * as regards to TCC, TCC implemented to check all resources, <br>
     *     at first, produce 'TOPIC'_TCC_TRY <br>
     *     if all resouces ok, produce 'TOPIC'_TCC_CONFIRM <br>
     *     any exception occured, produce 'TOPIC'_TCC_CANCEL <br>
     *     anyway, when isTcc = true, has to prepare 3 listeners to listener<br>
     * @param topic message topic
     * @param body  message body
     * @param svcs  the nameList of other listening domain service
     * @param callable  the service or controller handle the bisiness
     */
    Object produceReliably(Boolean isTcc, String topic, Object body, String[] svcs, Callable callable);

    /**
     *
     * @param svc  the name of listening domain service
     * @param message message body
     * @param runnable  listener handle the bisiness
     */
    void onConsumed(String svc, Object message, Runnable runnable);
}
