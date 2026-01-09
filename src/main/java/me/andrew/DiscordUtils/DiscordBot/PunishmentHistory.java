package me.andrew.DiscordUtils.DiscordBot;

import me.andrew.DiscordUtils.Plugin.DiscordUtils;
import me.andrew.DiscordUtils.Plugin.GUIs.Punishments.PunishmentsFilter;
import me.andrew.DiscordUtils.Plugin.Punishment;
import me.andrew.DiscordUtils.Plugin.PunishmentScopes;
import me.andrew.DiscordUtils.Plugin.PunishmentType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PunishmentHistory extends ListenerAdapter{
    private final DiscordUtils plugin;
    private final Map<Long, PaginationState> states = new HashMap<>();

    public PunishmentHistory(DiscordUtils plugin){
        this.plugin = plugin;

        //Task for auto removing users from the states map
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, ()->{
            long now = System.currentTimeMillis();
            states.entrySet().removeIf(entry -> now - entry.getValue().lastInteraction > TimeUnit.MINUTES.toMillis(1));
        },0L, 20L*60); //Runs each minute
    }

    public void displayPunishments(SlashCommandInteractionEvent event , UUID targetUUID, PunishmentsFilter filter, boolean self) throws SQLException{
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

    private void sendPage(InteractionHook hook, PaginationState state) throws SQLException{
        //Check if user still has the interaction going
        if(!states.containsKey(hook.getInteraction().getUser().getIdLong())){
            hook.setEphemeral(true).editOriginal("The connection *timed out*! Please run **/pshistory** again.").queue();
            return;
        }

        int limit = 6;
        int offset = (state.page-1) * limit;
        List<Punishment> punishments = getPunishmentsFromDB(state.targetUUID, state.filter, limit, offset);
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(state.targetUUID);

        EmbedBuilder embedBuilder = new EmbedBuilder();

        //If the player does /pshistory, displays their punishments
        if(state.self) embedBuilder.setTitle("Your Punishments ("+state.filter.name()+")");
        else embedBuilder.setTitle("\\"+targetPlayer.getName()+"'s Punishments ("+state.filter.name()+")");
        embedBuilder.setColor(Color.RED.asRGB());
        embedBuilder.setFooter("Page "+state.page);

        for(Punishment p : punishments){
            //Setting the title of the field
            String fieldTitle = getPunishmentTypeString(p.getPunishmentType()) +" ● "+ getPunishmentScopeString(p.getScope());
            if(state.filter == PunishmentsFilter.ALL){
                if(p.isRemoved()) fieldTitle += " ● *REMOVED* at **"+formatTime(p.getRemovedAt())+"**";
                else if(p.isActive()) fieldTitle += " ● *ACTIVE*";
                else fieldTitle += " ● *EXPIRED*";
            }

            //Setting the value of the field
            String fieldValue =
                    "**Issued at**: "+formatTime(p.getIssuedAt())+"\n"
                    + "\n" + "**Reason**: "+p.getReason()+"\n"
                    + "**Staff**: \\"+p.getStaff()+"\n";

            if(isPsTemporary(p) && p.isActive()) fieldValue += "**Expires At**: "+formatTime(p.getExpiresAt())+"\n";

            embedBuilder.addField(fieldTitle,fieldValue,true);
        }

        //Creating the buttons needed
        Button prevPage = Button.primary("page_prev", "◀").withDisabled(state.page == 1);
        Button nextPage = Button.primary("page_next", "▶").withDisabled(punishments.size() <= limit);
        Button filterAll = Button.secondary("filter_all", "ALL").withDisabled(state.filter == PunishmentsFilter.ALL);
        Button filterActive = Button.secondary("filter_active", "ACTIVE").withDisabled(state.filter == PunishmentsFilter.ACTIVE);
        Button filterExpired = Button.secondary("filter_expired", "EXPIRED").withDisabled(state.filter == PunishmentsFilter.EXPIRED);

        hook.editOriginalEmbeds(embedBuilder.build())
                .setComponents(ActionRow.of(prevPage, filterAll, filterActive, filterExpired, nextPage))
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event){
        long userId =  event.getUser().getIdLong();
        if(!states.containsKey(userId)) return;

        PaginationState state = states.get(userId);
        try{
            switch(event.getComponentId()){
                case "page_prev" -> state.page--;
                case "page_next" -> state.page++;
                case "filter_all" -> {
                    state.page = 1;
                    state.filter = PunishmentsFilter.ALL;
                }
                case "filter_active" ->{
                    state.page = 1;
                    state.filter = PunishmentsFilter.ACTIVE;
                }
                case "filter_expired" ->{
                    state.page = 1;
                    state.filter = PunishmentsFilter.EXPIRED;
                }
            }

            state.lastInteraction = System.currentTimeMillis();
            sendPage(event.getHook(), state);
        } catch (Exception e){
            throw new RuntimeException();
        }

        event.deferEdit().queue();
    }

    private String formatTime(long millis){
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        return time.format(formatter);
    }

    private List<Punishment> getPunishmentsFromDB(UUID targetUUID, PunishmentsFilter filter, int limit, int offset) throws SQLException{
        Connection dbConnection = plugin.getDatabaseManager().getConnection();

        String sql = "SELECT * FROM punishments WHERE uuid = ?";
        if(filter == PunishmentsFilter.ACTIVE) sql+=" AND active = 1";
        if(filter == PunishmentsFilter.EXPIRED) sql+=" AND active = 0";
        sql += " ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
            ps.setString(1, targetUUID.toString());
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try(ResultSet rs = ps.executeQuery()){
                List<Punishment> punishments = new ArrayList<>();
                while(rs.next()){
                    punishments.add(plugin.getDatabaseManager().mapPunishment(rs));
                }
                return punishments;
            }
        }
    }

    private String getPunishmentTypeString(PunishmentType type){
        return switch(type){
            case PERM_BAN -> "**PERMANENT BAN**";
            case TEMP_BAN -> "**TEMPORARY BAN**";
            case PERM_MUTE -> "**PERMANENT MUTE**";
            case TEMP_MUTE -> "**TEMP MUTE**";
            case KICK -> "**KICK**";
            case PERM_BAN_WARN -> "**PERMANENT BAN WARN**";
            case TEMP_BAN_WARN -> "**TEMPORARY BAN WARN**";
            case PERM_MUTE_WARN -> "**PERMANENT MUTE WARN**";
            case TEMP_MUTE_WARN -> "**TEMPORARY MUTE WARN**";
        };
    }
    private String getPunishmentScopeString(PunishmentScopes scope){
        return switch(scope){
            case MINECRAFT -> "*MINECRAFT*";
            case DISCORD -> "*DISCORD*";
            case GLOBAL -> "*GLOBAL*";
        };
    }
    private boolean isPsTemporary(Punishment p){
        return p.getPunishmentType() == PunishmentType.TEMP_BAN || p.getPunishmentType() == PunishmentType.TEMP_MUTE;
    }
}
