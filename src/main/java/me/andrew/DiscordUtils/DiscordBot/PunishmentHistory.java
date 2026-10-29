//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.PunishmentsFilter;
import me.andrew.DiscordUtils.Caching.Punishment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class PunishmentHistory extends ListenerAdapter{
    private final DiscordUtils plugin;
    private final Map<Long, PaginationState> states = new HashMap<>();

    public PunishmentHistory(DiscordUtils plugin){
        this.plugin = plugin;

        //Task for auto removing users from the states map
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, ()->{
            long now = System.currentTimeMillis();
            states.entrySet().removeIf(entry -> now - entry.getValue().lastInteraction() > TimeUnit.MINUTES.toMillis(1));
        },0L, 20L*60); //Runs each minute
    }

    public void displayPunishments(SlashCommandInteractionEvent event , UUID targetUUID, PunishmentsFilter filter, boolean self) {
        PaginationState initialState = new PaginationState(
                targetUUID,
                filter,
                1,
                self,
                System.currentTimeMillis()
        );

        states.put(event.getUser().getIdLong(), initialState);
        sendPage(event.getHook(), initialState);
    }

    private void sendPage(InteractionHook hook, PaginationState state) {
        //Check if user still has the interaction going
        if(!states.containsKey(hook.getInteraction().getUser().getIdLong())){
            hook.setEphemeral(true).editOriginal("The connection *timed out*! Please run **/pshistory** again.").queue();
            return;
        }

        int limit = 6;
        int offset = (state.page()-1) * limit;

        CopyOnWriteArrayList<Punishment> punishments = plugin.getPlayerPunishmentDataCache().getPlayerPunishments(state.targetUUID(), state.filter(), limit, offset);
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(state.targetUUID());

        EmbedBuilder embedBuilder = new EmbedBuilder();

        //If the player does /pshistory, displays their punishments
        if(state.self()) embedBuilder.setTitle("Your Punishments ("+state.filter().name()+")");
        else embedBuilder.setTitle("\\"+targetPlayer.getName()+"'s Punishments ("+state.filter().name()+")");
        embedBuilder.setColor(Color.RED.asRGB());
        embedBuilder.setFooter("Page "+state.page());

        for(Punishment p : punishments){
            //Setting the title of the field
            String fieldTitle = "**"+ p.punishmentType().getName()+"**" +" ● "+ "*"+p.scope().name()+"*";
            if(state.filter() == PunishmentsFilter.ALL){
                if(p.removed()) fieldTitle += " ● *REMOVED* at **"+formatTime(p.removedAt())+"**";
                else if(p.active()) fieldTitle += " ● *ACTIVE*";
                else fieldTitle += " ● *EXPIRED*";
            }

            //Setting the value of the field
            String fieldValue =
                    "**Issued at**: "+formatTime(p.createdAt())+"\n"
                    + "\n" + "**Reason**: "+p.reason()+"\n"
                    + "**Staff**: \\"+p.staff()+"\n";

            if(!p.punishmentType().isPermanent() && p.active()) fieldValue += "**Expires At**: "+formatTime(p.expiresAt())+"\n";

            embedBuilder.addField(fieldTitle,fieldValue,true);
        }

        //Creating the buttons needed
        Button prevPage = Button.primary("page_prev", "◀").withDisabled(state.page() == 1);
        Button nextPage = Button.primary("page_next", "▶").withDisabled(punishments.size() <= limit);
        Button filterAll = Button.secondary("filter_all", "ALL").withDisabled(state.filter() == PunishmentsFilter.ALL);
        Button filterActive = Button.secondary("filter_active", "ACTIVE").withDisabled(state.filter() == PunishmentsFilter.ACTIVE);
        Button filterExpired = Button.secondary("filter_expired", "EXPIRED").withDisabled(state.filter() == PunishmentsFilter.EXPIRED);

        hook.editOriginalEmbeds(embedBuilder.build())
                .setComponents(ActionRow.of(prevPage, filterAll, filterActive, filterExpired, nextPage))
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event){
        long userId =  event.getUser().getIdLong();
        if(!states.containsKey(userId)) return;

        PaginationState state = states.get(userId);
        int newPageNr = state.page();
        PunishmentsFilter newFilter = state.filter();

        switch(event.getComponentId()){
            case "page_prev" -> newPageNr--;
            case "page_next" -> newPageNr++;
            case "filter_all" -> {
                newPageNr = 1;
                newFilter = PunishmentsFilter.ALL;
            }
            case "filter_active" ->{
                newPageNr = 1;
                newFilter = PunishmentsFilter.ACTIVE;
            }
            case "filter_expired" ->{
                newPageNr = 1;
                newFilter = PunishmentsFilter.EXPIRED;
            }
        }

        sendPage(event.getHook(), new PaginationState(state.targetUUID(), newFilter, newPageNr, state.self(), System.currentTimeMillis()));
        event.deferEdit().queue();
    }

    private String formatTime(long millis){
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        return time.format(formatter);
    }
}

record PaginationState (UUID targetUUID, PunishmentsFilter filter, int page, boolean self, long lastInteraction){

}
