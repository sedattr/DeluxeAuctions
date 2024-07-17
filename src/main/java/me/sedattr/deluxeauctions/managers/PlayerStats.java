package me.sedattr.deluxeauctions.managers;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class PlayerStats {
    private final UUID player;

    private int wonAuctions = 0;
    private int lostAuctions = 0;
    private int totalBids = 0;
    private double highestBid = 0.0;
    private double spentMoney = 0.0;

    private int createdAuctions = 0;
    private int expiredAuctions = 0;
    private int soldAuctions = 0;
    private double earnedMoney = 0.0;
    private double totalFees = 0.0;

    public PlayerStats(UUID player) {
        this.player = player;
    }

    public void addWonAuction() {
        this.wonAuctions++;
    }

    public void addLostAuction() {
        this.lostAuctions++;
    }

    public void addTotalBids() {
        this.totalBids++;
    }

    public void addCreatedAuction() {
        this.createdAuctions++;
    }

    public void removeCreatedAuction() {
        this.createdAuctions--;
    }

    public void addExpiredAuction() {
        this.expiredAuctions++;
    }

    public void addSoldAuction() {
        this.soldAuctions++;
    }

    public void setHighestBid(double price) {
        if (price > this.highestBid)
            this.highestBid = price;
    }

    public void addSpentMoney(double amount) {
        this.spentMoney += amount;
    }

    public void addEarnedMoney(double amount) {
        this.earnedMoney += amount;
    }

    public void addTotalFees(double amount) {
        this.totalFees += amount;
    }
}
