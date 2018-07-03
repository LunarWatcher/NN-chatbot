package io.github.lunarwatcher.chatbot.bot.sites;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Host {
    STACKOVERFLOW("stackoverflow.com", "stackoverflow"),
    STACKEXCHANGE("stackexchange.com", "stackexchange"),
    METASTACKEXCHANGE("meta.stackexchange.com", "metastackexchange"),
    DISCORD("discord", false),
    TWITCH("twitch", false);

    @NotNull
    private String name;

    @Nullable
    private String mainSiteHost;
    @NotNull
    private String rawHost;
    @Nullable
    private String chatHost;
    private boolean connectable;

    Host(@NotNull String rawHost){
        this(rawHost, rawHost.replace(".", ""));
    }

    Host(@NotNull String rawHost, String name){
        this.rawHost = rawHost;
        this.name = name;
        this.chatHost = "https://chat." + rawHost;
        this.mainSiteHost = "https://" + rawHost;
        connectable = true;
    }

    Host(@NotNull String siteName, boolean connectable){
        this.name = siteName;
        this.rawHost = siteName;
        this.connectable = connectable;

        if(connectable){
            this.chatHost = "https://chat." + rawHost;
            this.mainSiteHost = "https://" + rawHost;
        }

    }

    Host(@NotNull String siteName, @NotNull String rawHost, @Nullable String chatHost, @Nullable String mainSiteHost, boolean connectable){
        this.name = siteName;
        this.rawHost = rawHost;
        this.chatHost = chatHost;
        this.mainSiteHost = mainSiteHost;
        this.connectable = connectable;
    }

    public boolean getConnectable(){
        return connectable;
    }

    @NotNull
    public String getRawHost(){
        return rawHost;
    }

    @Nullable
    public String getMainSiteHost(){
        return mainSiteHost;
    }

    @Nullable
    public String getChatHost(){
        return chatHost;
    }

    @NotNull
    public String getName(){
        return name;
    }

    @Nullable
    public static Host getHostForName(@NotNull String name){
        return getHostForName(name, true);
    }

    @Nullable
    public static Host getHostForName(@NotNull String name, boolean caseSensitive){
        for(Host host : Host.values()){
            if(caseSensitive) {
                if (host.getName().equals(name)) {
                    return host;
                }
            }else{
                if(host.getName().equalsIgnoreCase(name))
                    return host;
            }
        }

        return null;
    }
}
