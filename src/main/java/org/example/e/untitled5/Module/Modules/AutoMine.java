package org.example.e.untitled5.Module.Modules;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.e.untitled5.Module.AutoShaXTa.Utils.Modules;

import java.util.List;

public class AutoMine extends Modules {
    private static final int SEARCH_RADIUS = 24;
    private static final double REACH_SQR = 6 * 6;
    private final Minecraft mc = Minecraft.getInstance();

    private BlockPos targetDiamond = null;

    private static int useDelay = 4;
    private static int useTickCounter = 0;
    private static boolean movedWaiting = false;
    private static int afterMoveTicks = 0;

    private boolean repairing = false;
    private boolean clickedHopper = false;

    private boolean messageSend = false;
    private boolean buying = false;
    private boolean bought = false;

    public AutoMine(int key, String name) {
        super(key, name);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (mc.player == null || mc.level == null) return;
        mc.player.sendMessage(new StringTextComponent(String.valueOf(lastBuyPrice)),mc.player.getUUID());
        if ((isBottle())) {
            mc.player.sendMessage(new StringTextComponent("/home"), mc.player.getUUID());
            for (int i = 0; i < mc.player.inventory.getContainerSize(); i++) {
                Slot slot = mc.player.inventoryMenu.getSlot(i);
                ItemStack stack = slot.getItem();

                if ((stack.getItem() == Items.DIAMOND || stack.getItem() == Items.GOLD_INGOT) && stack.getCount() == 64) {
                    mc.player.drop(stack.copy(), true);
                    slot.set(ItemStack.EMPTY);
                }
            }
        }


        ItemStack mainHand = mc.player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof PickaxeItem) {
            int damage = mainHand.getDamageValue();
            int maxDamage = mainHand.getMaxDamage();
            double percentLeft = ((maxDamage - damage) * 100.0) / maxDamage;
            repairing = percentLeft <= 10;
        } else {
            repairing = false;
        }

        if (repairing) {
            mc.player.xRot = 90f;
            mc.options.keyAttack.setDown(false);
            handleRepair();
            return;
        }

        if (mc.screen != null) {
            resetTargets();
            mc.options.keyAttack.setDown(false);
            return;
        }

        BlockPos diamond = findTargetBlock(SEARCH_RADIUS);
        targetDiamond = null;

        if (diamond != null) {
            double distSqr = mc.player.blockPosition().distSqr(diamond);

            lookAt(diamond);
            targetDiamond = diamond;

            if (distSqr <= REACH_SQR) {
                mc.options.keyAttack.setDown(true);
                mc.options.keyJump.setDown(false);
            } else {
                mc.options.keyJump.setDown(diamond.getY() > mc.player.blockPosition().getY());
                mc.options.keyAttack.setDown(false);
            }
        } else {
            mc.options.keyAttack.setDown(false);
            mc.options.keyJump.setDown(false);
            targetDiamond = null;
        }

        handleRepair();
    }

    private void handleRepair() {
        if (mc.player == null) return;
        if (buying) return;

        int damage = mc.player.getMainHandItem().getDamageValue();
        int maxDamage = mc.player.getMainHandItem().getMaxDamage();
        repairing = ((maxDamage - damage) * 100.0 / maxDamage) <= 10;
        if (!repairing) return;

        int containerSlot = -1;
        for (int i = 0; i < mc.player.containerMenu.slots.size(); i++) {
            Slot slot = mc.player.containerMenu.getSlot(i);
            ItemStack s = slot.getItem();
            if (!s.isEmpty() && s.getItem() == Items.EXPERIENCE_BOTTLE) {
                containerSlot = i;
                break;
            }
        }

        if (containerSlot != -1) {
            if (movedWaiting) {
                if (afterMoveTicks-- > 0) return;
                movedWaiting = false;
                return;
            }

            ItemStack offhand = mc.player.getOffhandItem();
            if (offhand.isEmpty() || offhand.getItem() != Items.EXPERIENCE_BOTTLE) {
                try {
                    mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, containerSlot, 40, ClickType.SWAP, mc.player);
                    movedWaiting = true;
                    afterMoveTicks = 3;
                } catch (Exception t) {
                    System.out.println("move to offhand failed: " + t);
                }
                return;
            }
            useTickCounter++;
            if (useTickCounter >= useDelay) {
                useTickCounter = 0;
                mc.player.swing(Hand.OFF_HAND);
                mc.gameMode.useItem(mc.player, mc.level, Hand.OFF_HAND);
            }
        }
    }

    public boolean isBottle() {
        if (mc.player != null) {
            for (int i = 0; i < mc.player.inventory.getContainerSize(); i++) {
                ItemStack stack = mc.player.inventory.getItem(i);
                if (!stack.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onInput(InputUpdateEvent e) {
        if (mc.player == null) return;

        if (repairing) {
            e.getMovementInput().forwardImpulse = 0.0F;
            e.getMovementInput().leftImpulse = 0.0F;
            return;
        }

        if (targetDiamond != null) {
            e.getMovementInput().forwardImpulse = 1.0F;
            e.getMovementInput().leftImpulse = 0.0F;
        } else {
            e.getMovementInput().forwardImpulse = 0.0F;
            e.getMovementInput().leftImpulse = 0.0F;
        }
    }

    private void resetTargets() {
        targetDiamond = null;
    }

    private BlockPos findTargetBlock(int radius) {
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos nearestBlock = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (!mc.level.isLoaded(pos)) continue;

                    Block block = mc.level.getBlockState(pos).getBlock();
                    if (block == Blocks.AIR) continue;

                    if (block == Blocks.GOLD_ORE  || block == Blocks.DIAMOND_ORE) {
                        double dist = playerPos.distSqr(pos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearestBlock = pos;
                        }
                    }
                }
            }
        }
        return nearestBlock;
    }

    private void lookAt(BlockPos target) {
        double dx = target.getX() + 0.5 - mc.player.getX();
        double dy = target.getY() + 0.5 - (mc.player.getY() + mc.player.getEyeHeight());
        double dz = target.getZ() + 0.5 - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        mc.player.yRot = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90F);
        mc.player.xRot = (float) (-Math.toDegrees(Math.atan2(dy, dist)));
    }

    private int lastBuyPrice = -1;

}