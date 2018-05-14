package io.github.lunarwatcher.chatbot.bot.command;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CmdInfo{
    List<String> names;
    public CommandGroup group;

    /**
     * Constructor for commands not defining the command group. Note that this should only be used by taught commands,
     * as the other commands are routed through the other constructor by {@link CommandCenter#addCommand}.
     *
     * @param name The command name
     * @param aliases A list of the commands aliases
     */
    public CmdInfo(@NonNull String name, List<String> aliases){
        this(name, aliases, CommandGroup.COMMON);
    }
    public CmdInfo(@NonNull String name, List<String> aliases, CommandGroup group){
        this.names = new ArrayList<>();
        if(aliases != null)
            names.addAll(aliases);

        this.group = group;
        names.add(name);
    }

    /**
     * Manual implementation of the equals method to find matches when the objects aren't
     * 100% the same.
     * @param other The object to compare
     * @return Whether or not the object can be matched
     */
    public boolean equals(Object other){
        if(!(other instanceof CmdInfo) && !(other instanceof String))
            return false;

        if(other instanceof CmdInfo) {
            for (int i = 0; i < names.size(); i++) {
                for (int j = 0; j < ((CmdInfo) other).names.size(); j++) {
                    if (((CmdInfo) other).names.get(j).toLowerCase().equals(names.get(i).toLowerCase())) {
                        return true;
                    }
                }
            }
        }else{
            for (String name : names) {
                if (name.toLowerCase().equals(((String)other).toLowerCase()))
                    return true;
            }
        }

        return false;
    }

}