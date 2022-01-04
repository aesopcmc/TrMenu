package me.arasple.mc.trmenu.module.internal.command.impl

import me.arasple.mc.trmenu.api.TrMenuAPI
import me.arasple.mc.trmenu.api.event.MenuOpenEvent
import me.arasple.mc.trmenu.module.display.Menu
import me.arasple.mc.trmenu.module.internal.command.CommandExpresser
import me.arasple.mc.trmenu.module.internal.data.Metadata
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.subCommand
import taboolib.platform.util.sendLang

/**
 * @author Arasple
 * @date 2021/1/28 20:11
 */
object CommandOpen : CommandExpresser {

    // menu open [menuId] [player] [args...]
    override val command = subCommand {
        // menuId
        dynamic {
            suggestion<CommandSender> { _, _ ->
                Menu.menus.map { it.id }
            }

            execute<CommandSender> { sender, context, argument ->
                val split = context.argument(0).split(":")
                val menu = TrMenuAPI.getMenuById(split[0])
                val page = split.getOrNull(1)?.toIntOrNull() ?: 0
                val player = if (sender is Player) sender else null

                menu ?: return@execute sender.sendLang("Command-Open-Unknown-Menu", argument)
                if (player == null || !player.isOnline) {
                    return@execute sender.sendLang("Command-Open-Unknown-Player", "CONSOLE")
                }

                menu.open(player, page, MenuOpenEvent.Reason.PLAYER_COMMAND) {
                    it.arguments = arrayOf()
                }

            }

            // player
            dynamic(optional = true) {
                suggestion<CommandSender>(uncheck = true) { _, _ ->
                    Bukkit.getOnlinePlayers().map { it.name }
                }

                execute<CommandSender> { sender, context, argument ->
                    val split = context.argument(-1).split(":")
                    val menu = TrMenuAPI.getMenuById(split[0])
                    val page = split.getOrNull(1)?.toIntOrNull() ?: 0
                    val players = (if (argument.contains(" ")) argument.substringBefore(" ") else argument).split(":")
                    val player = Bukkit.getPlayerExact(players[0]) ?: sender as? Player
                    val agent = if (players.size > 1) Bukkit.getPlayerExact(players[1]) else null
                    val arguments = runCatching {
                        argument.split(" ").let {
                            it.slice(1 until it.size)
                        }
                    }.getOrNull()?.toTypedArray() ?: arrayOf()

                    menu ?: return@execute sender.sendLang("Command-Open-Unknown-Menu", context.argument(-1))
                    if (player == null || !player.isOnline) {
                        return@execute sender.sendLang("Command-Open-Unknown-Player", context.argument(0))
                    }
                    if (players.size > 1 && (agent == null || !agent.isOnline)) {
                        return@execute sender.sendLang("Command-Open-Unknown-Player", players[1])
                    }

                    menu.open(player, page, MenuOpenEvent.Reason.PLAYER_COMMAND) {
                        it.agent = if (agent != null ) agent else player
                        if (!Metadata.byBukkit(player, "FORCE_ARGS") || (arguments.isNotEmpty())) {
                            if (arguments.isEmpty()) {
                                it.arguments = it.implicitArguments.clone()
                                it.implicitArguments = arrayOf()
                            } else {
                                it.arguments = arguments
                            }
                        }
                    }
                }
            }
        }
    }

}