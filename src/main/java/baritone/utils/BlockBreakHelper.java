/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.api.BaritoneAPI;
import baritone.api.utils.IPlayerContext;
import baritone.utils.accessor.IPlayerControllerMP;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * @author Brady
 * @since 8/25/2018
 */
public final class BlockBreakHelper {
    // base ticks between block breaks caused by tick logic
    private static final int BASE_BREAK_DELAY = 1;

    /**
     * How many ticks in a row Baritone may fail to click before a break in progress is given up. A tick without a
     * click is one where the aim was off the block (see {@link BreakStreak}). Under water the player's eye moves every
     * tick, so the aim, which is one tick behind the eye, is off for a few ticks at a time: 4 in a row at most in
     * the 300 ticks of the toolset-break-speed recording. Half a second is far longer than that and short enough
     * that a break Baritone has moved on from is not resumed.
     */
    static final int MAX_MISSED_TICKS = 10;

    private final IPlayerContext ctx;
    private final BreakStreak streak = new BreakStreak();
    private int breakDelayTimer = 0;

    BlockBreakHelper(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public void stopBreakingBlock() {
        // The player controller will never be null, but the player can be
        if (ctx.player() != null && streak.inProgress()) {
            ctx.playerController().setHittingBlock(false);
            ctx.playerController().resetBlockRemoving();
            streak.end();
        }
    }

    public void tick(boolean isLeftClick) {
        if (breakDelayTimer > 0) {
            breakDelayTimer--;
            return;
        }
        HitResult trace = ctx.objectMouseOver();
        boolean isBlockTrace = trace != null && trace.getType() == HitResult.Type.BLOCK;

        if (isLeftClick && isBlockTrace) {
            ctx.playerController().setHittingBlock(streak.inProgress());
            if (ctx.playerController().hasBrokenBlock()) {
                ctx.playerController().syncHeldItem();
                ctx.playerController().clickBlock(((BlockHitResult) trace).getBlockPos(), ((BlockHitResult) trace).getDirection());
                ctx.player().swing(InteractionHand.MAIN_HAND);
            } else {
                if (ctx.playerController().onPlayerDamageBlock(((BlockHitResult) trace).getBlockPos(), ((BlockHitResult) trace).getDirection())) {
                    ctx.player().swing(InteractionHand.MAIN_HAND);
                }
                if (ctx.playerController().hasBrokenBlock()) { // block broken this tick
                    // break delay timer only applies for multi-tick block breaks like vanilla
                    breakDelayTimer = BaritoneAPI.getSettings().blockBreakSpeed.value - BASE_BREAK_DELAY;
                    // must reset controller's destroy delay to prevent the client from delaying itself unnecessarily
                    ((IPlayerControllerMP) ctx.minecraft().gameMode).setDestroyDelay(0);
                }
            }
            // if true, we're breaking a block. if false, we broke the block this tick
            streak.hit(!ctx.playerController().hasBrokenBlock());
            // this value will be reset by the MC client handling mouse keys
            // since we're not spoofing the click keybind to the client, the client will stop the break if isDestroyingBlock is true
            // we store and restore this value on the next tick to determine if we're breaking a block
            ctx.playerController().setHittingBlock(false);
        } else {
            streak.miss();
        }
    }

    /**
     * Whether the block Baritone is breaking is mid-break, as far as the next tick's click is concerned.
     * <p>
     * {@code tick} tells the client a break is in progress only when the previous tick clicked
     * ({@code setHittingBlock(inProgress())}); otherwise it clicks as a fresh start, and the game's
     * {@code MultiPlayerGameMode#startDestroyBlock} sets the break progress back to 0 when it is not already destroying
     * (MultiPlayerGameMode.java:169-197 in 1.21.11). A tick without a click, because the block was not under the aim
     * that tick, therefore used to throw away everything dug so far. A short run of such ticks is not a decision to
     * stop: if the next click is on the same block the break carries on, and if it is on another block the game aborts
     * the old one and starts the new one itself.
     */
    static final class BreakStreak {
        private boolean inProgress;
        private int missedTicks;

        boolean inProgress() {
            return inProgress;
        }

        /** A tick that clicked a block; {@code breaking} is false when the click broke it. */
        void hit(boolean breaking) {
            inProgress = breaking;
            missedTicks = 0;
        }

        /** A tick that did not click: the aim was off, or Baritone was not trying to break anything. */
        void miss() {
            if (++missedTicks > MAX_MISSED_TICKS) {
                inProgress = false;
            }
        }

        void end() {
            inProgress = false;
            missedTicks = 0;
        }
    }
}
