package ru.itmo.episland.energy;

public interface AccountingGateway {
    boolean deliver(EnergyShift shift);
}
