package com.cmulagent.resource;

import com.cmulagent.event.AgentEvent;
import com.cmulagent.event.AgentEventType;
import com.cmulagent.event.AgentWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class EndpointLoadBalancer {

    private static final Logger log = LoggerFactory.getLogger(EndpointLoadBalancer.class);

    private final Map<String, ResourceSlot> slots = new ConcurrentHashMap<>();
    private final AgentWebSocketHandler webSocketHandler;

    public EndpointLoadBalancer(AgentWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public boolean acquireSlot(String endpoint, String agentId) {
        ResourceSlot slot = slots.computeIfAbsent(endpoint, k -> createDefaultSlot(k));
        synchronized (slot) {
            if (slot.getUsedCapacity() < slot.getMaxCapacity()) {
                slot.setUsedCapacity(slot.getUsedCapacity() + 1);
                slot.setAvailable(slot.getUsedCapacity() < slot.getMaxCapacity());
                emitSlotChanged(slot);
                log.debug("Slot acquired: endpoint={}, used={}/{}", endpoint, slot.getUsedCapacity(), slot.getMaxCapacity());
                return true;
            }
        }
        log.warn("Slot acquire failed (full): endpoint={}, agentId={}", endpoint, agentId);
        return false;
    }

    public void releaseSlot(String endpoint, String agentId) {
        ResourceSlot slot = slots.get(endpoint);
        if (slot != null) {
            synchronized (slot) {
                if (slot.getUsedCapacity() > 0) {
                    slot.setUsedCapacity(slot.getUsedCapacity() - 1);
                    slot.setAvailable(true);
                    emitSlotChanged(slot);
                    log.debug("Slot released: endpoint={}, used={}/{}", endpoint, slot.getUsedCapacity(), slot.getMaxCapacity());
                }
            }
        }
    }

    public List<ResourceSlot> getAvailableSlots() {
        return slots.values().stream()
                .filter(s -> {
                    synchronized (s) {
                        return s.getUsedCapacity() < s.getMaxCapacity();
                    }
                })
                .collect(Collectors.toList());
    }

    public ResourceSlot getSlot(String id) {
        // 先尝试通过槽位UUID查找
        for (ResourceSlot slot : slots.values()) {
            if (slot.getId().equals(id)) {
                return slot;
            }
        }
        // 再尝试通过endpoint key查找
        return slots.get(id);
    }

    public List<ResourceSlot> getAllSlots() {
        return new ArrayList<>(slots.values());
    }

    public void registerEndpoint(String endpoint, String name, int maxCapacity) {
        ResourceSlot slot = ResourceSlot.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .type("LLM_ENDPOINT")
                .maxCapacity(maxCapacity)
                .usedCapacity(0)
                .available(true)
                .build();
        slots.put(endpoint, slot);
        log.info("Endpoint registered: {} -> {} (maxCapacity={})", endpoint, name, maxCapacity);
        emitSlotChanged(slot);
    }

    private ResourceSlot createDefaultSlot(String endpoint) {
        int maxCap = endpoint.contains(":11434") || endpoint.contains("localhost") ? 3 : 10;
        return ResourceSlot.builder()
                .id(UUID.randomUUID().toString())
                .name(extractHost(endpoint))
                .type("LLM_ENDPOINT")
                .maxCapacity(maxCap)
                .usedCapacity(0)
                .available(true)
                .build();
    }

    private String extractHost(String endpoint) {
        try {
            URI uri = new URI(endpoint);
            int port = uri.getPort() > 0 ? uri.getPort() : (uri.getScheme() != null && uri.getScheme().equals("https") ? 443 : 80);
            return uri.getHost() + ":" + port;
        } catch (Exception e) {
            return endpoint;
        }
    }

    private void emitSlotChanged(ResourceSlot slot) {
        try {
            AgentEvent event = AgentEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .type(AgentEventType.RESOURCE_SLOT_CHANGED)
                    .source(slot.getId())
                    .data(Map.of(
                            "slotId", slot.getId(),
                            "endpoint", slot.getName(),
                            "usedCapacity", slot.getUsedCapacity(),
                            "maxCapacity", slot.getMaxCapacity(),
                            "available", slot.isAvailable()
                    ))
                    .timestamp(Instant.now())
                    .build();
            webSocketHandler.publishEvent(event);
        } catch (Exception e) {
            log.warn("Failed to emit slot changed event", e);
        }
    }
}