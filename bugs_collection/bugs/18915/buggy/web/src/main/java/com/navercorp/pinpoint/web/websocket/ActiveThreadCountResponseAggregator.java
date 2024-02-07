/*
 *
 *  * Copyright 2014 NAVER Corp.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package com.navercorp.pinpoint.web.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.navercorp.pinpoint.common.util.AgentLifeCycleState;
import com.navercorp.pinpoint.web.service.AgentService;
import com.navercorp.pinpoint.web.vo.AgentActiveThreadCount;
import com.navercorp.pinpoint.web.vo.AgentActiveThreadCountList;
import com.navercorp.pinpoint.web.vo.AgentInfo;
import com.navercorp.pinpoint.web.vo.AgentStatus;
import com.navercorp.pinpoint.web.websocket.message.PinpointWebSocketMessageConverter;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Author Taejin Koo
 */
public class ActiveThreadCountResponseAggregator implements PinpointWebSocketResponseAggregator {

    private static final String APPLICATION_NAME = "applicationName";
    private static final String ACTIVE_THREAD_COUNTS = "activeThreadCounts";
    private static final String TIME_STAMP = "timeStamp";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String applicationName;
    private final AgentService agentService;
    private final Timer timer;

    private final Object workerManagingLock = new Object();
    private final List<WebSocketSession> webSocketSessions = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<String, ActiveThreadCountWorker> activeThreadCountWorkerRepository = new ConcurrentHashMap<>();

    private final Object aggregatorLock = new Object();
    private final PinpointWebSocketMessageConverter messageConverter;

    private volatile boolean isStopped = false;
    private WorkerActiveManager workerActiveManager;

    private Map<String, AgentActiveThreadCount> activeThreadCountMap = new HashMap<>();

    public ActiveThreadCountResponseAggregator(String applicationName, AgentService agentService, Timer timer) {
        this.applicationName = applicationName;
        this.agentService = agentService;

        this.timer = timer;

        this.messageConverter = new PinpointWebSocketMessageConverter();
    }

    @Override
    public void start() {
        synchronized (workerManagingLock) {
            workerActiveManager = new WorkerActiveManager(this, agentService, timer);
        }
    }

    @Override
    public void stop() {
        synchronized (workerManagingLock) {
            isStopped = true;

            if (workerActiveManager != null) {
                this.workerActiveManager.close();
            }

            for (ActiveThreadCountWorker worker : activeThreadCountWorkerRepository.values()) {
                if (worker != null) {
                    worker.stop();
                }
            }

            activeThreadCountWorkerRepository.clear();
        }
    }

    @Override
    public void addWebSocketSession(WebSocketSession webSocketSession) {
        if (webSocketSession == null) {
            return;
        }

        logger.info("addWebSocketSession. applicationName:{}, webSocketSession:{}", applicationName, webSocketSession);

        List<AgentInfo> agentInfoList = agentService.getRecentAgentInfoList(applicationName);
        synchronized (workerManagingLock) {
            if (isStopped) {
                return;
            }

            for (AgentInfo agentInfo : agentInfoList) {
                AgentStatus agentStatus = agentInfo.getStatus();
                if (agentStatus != null && agentStatus.getState() != AgentLifeCycleState.UNKNOWN) {
                    activeWorker(agentInfo);
                } else if (agentService.isConnected(agentInfo)) {
                    activeWorker(agentInfo);
                }
            }

            boolean added = webSocketSessions.add(webSocketSession);
            if (added && webSocketSessions.size() == 1) {
                workerActiveManager.startAgentCheckJob();
            }
        }
    }

    // return when aggregator cleared.
    @Override
    public boolean removeWebSocketSessionAndGetIsCleared(WebSocketSession webSocketSession) {
        if (webSocketSession == null) {
            return false;
        }

        logger.info("removeWebSocketSessionAndGetIsCleared. applicationName{}, webSocketSession:{}", applicationName, webSocketSession);

        synchronized (workerManagingLock) {
            if (isStopped) {
                return true;
            }

            boolean removed = webSocketSessions.remove(webSocketSession);
            if (removed && webSocketSessions.size() == 0) {
                for (ActiveThreadCountWorker activeThreadCountWorker : activeThreadCountWorkerRepository.values()) {
                    activeThreadCountWorker.stop();
                }
                activeThreadCountWorkerRepository.clear();
                return true;
            }
        }

        return false;
    }

    @Override
    public void addActiveWorker(AgentInfo agentInfo) {
        logger.info("activeWorker applicationName:{}, agentId:{}", applicationName, agentInfo.getAgentId());

        if (!applicationName.equals(agentInfo.getApplicationName())) {
            return;
        }

        synchronized (workerManagingLock) {
            if (isStopped) {
                return;
            }
            activeWorker(agentInfo);
        }
    }

    private void activeWorker(AgentInfo agentInfo) {
        String agentId = agentInfo.getAgentId();

        synchronized (workerManagingLock) {
            ActiveThreadCountWorker worker = activeThreadCountWorkerRepository.get(agentId);
            if (worker == null) {
                worker = new ActiveThreadCountWorker(agentService, agentInfo, this, workerActiveManager);
                worker.start(agentInfo);

                activeThreadCountWorkerRepository.put(agentId, worker);
            } else {
                worker.reactive(agentInfo);
            }
        }
    }

    @Override
    public void response(AgentActiveThreadCount activeThreadCount) {
        if (activeThreadCount == null) {
            return;
        }

        synchronized (aggregatorLock) {
            this.activeThreadCountMap.put(activeThreadCount.getAgentId(), activeThreadCount);
        }
    }

    @Override
    public void flush() throws Exception {
        logger.info("flush started. applicationName:{}", applicationName);

        if (isStopped) {
            return;
        }

        AgentActiveThreadCountList response = new AgentActiveThreadCountList();
        synchronized (aggregatorLock) {
            for (ActiveThreadCountWorker activeThreadCountWorker : activeThreadCountWorkerRepository.values()) {
                String agentId = activeThreadCountWorker.getAgentId();

                AgentActiveThreadCount agentActiveThreadCount = activeThreadCountMap.get(agentId);
                if (agentActiveThreadCount != null) {
                    response.add(agentActiveThreadCount);
                } else {
                    response.add(activeThreadCountWorker.getDefaultFailedResponse());
                }
            }
            activeThreadCountMap = new HashMap<>(activeThreadCountWorkerRepository.size());
        }

        flush0(response);
    }

    private void flush0(AgentActiveThreadCountList activeThreadCountList) {
        Map resultMap = createResultMap(activeThreadCountList, System.currentTimeMillis());
        try {
            TextMessage responseTextMessage = new TextMessage(messageConverter.getResponseTextMessage(ActiveThreadCountHandler.API_ACTIVE_THREAD_COUNT, resultMap));

            for (WebSocketSession webSocketSession : webSocketSessions) {
                try {
                    logger.debug("flush webSocketSession:{}, response:{}", webSocketSession, responseTextMessage);
                    webSocketSession.sendMessage(responseTextMessage);
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        } catch (JsonProcessingException e) {
            logger.warn("json convert failed. original:{}, message:{}.", resultMap, e.getMessage(), e);
        }
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    private Map createResultMap(AgentActiveThreadCountList activeThreadCount, long timeStamp) {
        Map<String, Object> response = new HashMap<>();

        response.put(APPLICATION_NAME, applicationName);
        response.put(ACTIVE_THREAD_COUNTS, activeThreadCount);
        response.put(TIME_STAMP, timeStamp);

        return response;
    }

    private String createEmptyResponseMessage(String applicationName, long timeStamp) {
        StringBuilder emptyJsonMessage = new StringBuilder();
        emptyJsonMessage.append("{");
        emptyJsonMessage.append("\"").append(APPLICATION_NAME).append("\"").append(":").append("\"").append(applicationName).append("\"").append(",");
        emptyJsonMessage.append("\"").append(ACTIVE_THREAD_COUNTS).append("\"").append(":").append("{}").append(",");
        emptyJsonMessage.append("\"").append(TIME_STAMP).append("\"").append(":").append(timeStamp);
        emptyJsonMessage.append("}");

        return emptyJsonMessage.toString();
    }

}
