package io.github.pikczu77.upgrades.ability;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Trades offered by a sniffed mob or block. Lives only on the server, the client gets a normal merchant screen.
 */
public class SniffedMerchant implements Merchant {
	private final MerchantOffers offers;
	private final Predicate<Player> valid;
	private @Nullable Player trader;

	public SniffedMerchant(MerchantOffers offers, Predicate<Player> valid) {
		this.offers = offers;
		this.valid = valid;
	}

	@Override
	public void setTradingPlayer(@Nullable Player player) {
		this.trader = player;
	}

	@Override
	public @Nullable Player getTradingPlayer() {
		return this.trader;
	}

	@Override
	public MerchantOffers getOffers() {
		return this.offers;
	}

	@Override
	public void overrideOffers(MerchantOffers offers) {
	}

	@Override
	public void notifyTrade(MerchantOffer offer) {
		offer.increaseUses();
	}

	@Override
	public void notifyTradeUpdated(ItemStack stack) {
	}

	@Override
	public int getVillagerXp() {
		return 0;
	}

	@Override
	public void overrideXp(int xp) {
	}

	@Override
	public boolean showProgressBar() {
		return false;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return SoundEvents.VILLAGER_YES;
	}

	@Override
	public boolean isClientSide() {
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.trader == player && this.valid.test(player);
	}
}
