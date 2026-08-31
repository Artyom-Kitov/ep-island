package ru.itmo.episland.energy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatedAccountingGateway implements AccountingGateway {
    private final boolean simulateFailure;

    public SimulatedAccountingGateway(
        @Value("${ep-island.simulate-accounting-failure:false}") boolean simulateFailure
    ) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public boolean deliver(EnergyShift shift) {
        return !simulateFailure;
    }
}
