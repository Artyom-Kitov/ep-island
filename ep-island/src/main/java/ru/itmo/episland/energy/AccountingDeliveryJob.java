package ru.itmo.episland.energy;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountingDeliveryJob {
    private final EnergyService energyService;

    public AccountingDeliveryJob(EnergyService energyService) {
        this.energyService = energyService;
    }

    @Scheduled(fixedDelayString = "${ep-island.accounting-retry-interval-ms:30000}")
    public void retryPendingDeliveries() {
        energyService.retryPendingDeliveries();
    }
}
