package ru.itmo.episland.energy;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.itmo.episland.events.LiveUpdateScope;
import ru.itmo.episland.events.LiveUpdateService;

@Component
public class AccountingDeliveryJob {
    private final EnergyService energyService;
    private final LiveUpdateService liveUpdates;

    public AccountingDeliveryJob(EnergyService energyService, LiveUpdateService liveUpdates) {
        this.energyService = energyService;
        this.liveUpdates = liveUpdates;
    }

    @Scheduled(fixedDelayString = "${ep-island.accounting-retry-interval-ms:30000}")
    public void retryPendingDeliveries() {
        if (energyService.retryPendingDeliveries() > 0) {
            liveUpdates.publish(LiveUpdateScope.ENERGY, null);
        }
    }
}
