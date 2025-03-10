 package com.anticirculatory.villageDisplay.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ServerWorldAccess;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

 public class VillageDisplayClient implements ClientModInitializer {
     public static final Logger LOGGER = LoggerFactory.getLogger("anticirculatory-villager-display");
     public static final List<String> entityStrings = new ArrayList<String>();
     private static KeyBinding keyBinding;
     private static KeyBinding keyBinding2;

     private static long lastTime = System.currentTimeMillis();
     private static boolean enabled = false;
     private static boolean filtering = true;
     private static boolean showChunks = false;

     private static Set<String> filterNames = new HashSet<String>();

     private static void renderHud(DrawContext drawContext, RenderTickCounter renderTickCounter) {
         if(!(showChunks || enabled)) {
             return;
         }

         MinecraftClient client = MinecraftClient.getInstance();

         try {
             if (showChunks) {
                 ClientPlayerEntity player = client.player;

                 double playerX = player.getX();
                 double playerZ = player.getZ();

                 int playerChunkX = (int) Math.floor(playerX / 16.0);
                 int playerChunkZ = (int) Math.floor(playerZ / 16.0);

                 IntegratedServer server = client.getServer();
                 ServerWorld serverWorld = server.getOverworld();

                 Set<Long> loaded = new HashSet<Long>();
                 for(int chunkX = -32; chunkX <= 32; chunkX++) {
                     for(int chunkZ = -32; chunkZ <= 32; chunkZ++) {
                         if(Math.abs(playerChunkX - chunkX) >= 16
                                 && Math.abs(playerChunkZ - chunkZ) >= 16) {
                             long chunkPos = ChunkPos.toLong(chunkX, chunkZ);
                             if(serverWorld.isChunkLoaded(chunkPos)) {
                                 loaded.add(chunkPos);
                             }
                         }
                     }
                 }

                 List<ChunkPos> entityChunks = new ArrayList<ChunkPos>();

                 int chunksLoaded = 0;
                 for(Long chunkPos : loaded) {
                     ChunkPos cp = new ChunkPos(chunkPos);

                     boolean isEntityProcessing = true;

                     for(int x = cp.x - 2; x <= (cp.x + 2); x++) {
                         if(!isEntityProcessing) {
                             break;
                         }

                         for(int z = cp.z - 2; z <= (cp.z + 2); z++) {
                            if(!loaded.contains(ChunkPos.toLong(x, z))) {
                                isEntityProcessing = false;
                                break;
                            }
                         }
                     }

                     if(isEntityProcessing) {
                         entityChunks.add(cp);
                     }
                 }

                 entityChunks.sort(new Comparator<ChunkPos>() {
                     @Override
                     public int compare(ChunkPos s1, ChunkPos s2) {
                         if (s1.x == s2.x) {
                             return Integer.compare(s1.z, s2.z);
                         } else {
                             return Integer.compare(s1.x, s2.x);
                         }
                     }
                 });

                 drawContext.drawText(client.textRenderer, "Chunks: " + entityChunks.size(), 0, 0, 16777215, false);

                 int x = 80;
                 int y = 9;

                 for(ChunkPos entityChunk : entityChunks) {
                     drawContext.drawText(client.textRenderer, String.format("%d, %d", entityChunk.x, entityChunk.z), x, y, 16777215, false);
                     y += 9;
                     if(y > 190) {
                         y = 0;
                         x += 45;
                     }
                 }
             } else if(enabled) {
                 ClientWorld world = client.world;
                 entityStrings.clear();

                 int entityCount = 0;
                 for(Entity entity : world.getEntities()) {
                     if(!entity.isAlive()) {
                         continue;
                     }

                     if(filterNames.isEmpty()) {
                         filterNames.add("Cow");
                         filterNames.add("Sheep");
                         filterNames.add("Pig");
                         filterNames.add("Frog");
                         filterNames.add("Wolf");
                         filterNames.add("Chick");
                         filterNames.add("Minec");
                         filterNames.add("Spruc");
                         filterNames.add("Bat");
                         filterNames.add("Egg");
                     }

                     String str = entity.getName().getString();
                     String firstFiveChars = str.length() >= 5 ? str.substring(0, 5) : str;

                     if(filterNames.contains(firstFiveChars)) {
                         continue;
                     }

                     if("Playe".equals(firstFiveChars) || "mbasa".equals(firstFiveChars)) {
                         entityStrings.add("");
                         entityStrings.add("");
                     } else {
                         if(filtering) {
                             int theY = (int) entity.lastRenderY;

                             if (theY == 31) {
                                 continue;
                             } else if (theY == 53) {
                                 int theX = (int) entity.lastRenderX;
                                 int theZ = (int) entity.lastRenderZ;

                                 if (theX >= 308 && theX <= 328 && theZ >= -393 && theZ <= -373) {
                                     continue;
                                 }
                             }
                         }

                         entityStrings.add(String.format("%s %d %d %d", firstFiveChars, (int) entity.lastRenderX, (int) entity.lastRenderY, (int) entity.lastRenderZ));
                         entityCount++;
                     }
                 }

                 int x = 0;
                 int y = 54;

                 if(!entityStrings.isEmpty()) {
                     for(String entityString : entityStrings) {
                         drawContext.drawText(client.textRenderer, entityString, x, y, 16777215, false);
                         y += 9;
                         if(y > 190) {
                             y = 0;
                             x += 120;
                         }
                     }

                     x = 0;
                     y = 54;
                     drawContext.drawText(client.textRenderer, "Count: " + entityCount, x, y, 16777215, false);
                 } else {
                     drawContext.drawText(client.textRenderer, "Anticirculatory!", 0, 0, 16777215, false);
                 }
             }
         } catch (Exception e) {
             String errorMessage = e.toString();
             int errorLength = errorMessage.length();

             int start = 0;
             int end = 70;
             int y = 0;
             while(end <= errorLength) {
                 String sub = "";

                 if(end >= errorLength) {
                     sub = errorMessage.substring(start);
                 } else {
                     sub = errorMessage.substring(start, end);
                 }

                 drawContext.drawText(client.textRenderer, sub, 0, y, 16777215, false);
                 y += 10;
                 start += 70;
                 end += 70;
             }
         }
     }

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(VillageDisplayClient::renderHud);

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.anticirculatory-villager-display.f8", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_F8, // The keycode of the key
                "category.anticirculatory-villager-display.f8" // The translation key of the keybinding's category.
        ));

        keyBinding2 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.anticirculatory-villager-display.f7", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_F7, // The keycode of the key
                "category.anticirculatory-villager-display.f7" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                long currentTime = System.currentTimeMillis();

                if((currentTime - lastTime) > 1000) {
                    lastTime = currentTime;
                    enabled = !enabled;
                    showChunks = false;
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding2.wasPressed()) {
                long currentTime = System.currentTimeMillis();

                if((currentTime - lastTime) > 1000) {
                    lastTime = currentTime;

                    if(enabled) {
                        filtering = !filtering;
                    } else {
                        showChunks = !showChunks;
                    }
                }
            }
        });
    }
}
