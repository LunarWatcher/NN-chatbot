package io.github.lunarwatcher.chatbot.bot;


public class ReplyBuilder {
    public boolean discord = false;
    private boolean fixed;

    StringBuilder builder;
    public ReplyBuilder() {
        builder = new StringBuilder();
    }

    public ReplyBuilder(String initial){
        builder = new StringBuilder(initial);
    }

    public ReplyBuilder(boolean discord){
        builder = new StringBuilder();
        this.discord = discord;
    }

    public ReplyBuilder(String initial, boolean discord){
        builder = new StringBuilder(initial);
        this.discord = discord;
    }

    public ReplyBuilder fixedInput(){
        //For-loop as adding four spaces directly doesn't work for some reason
        if(!discord) {
            for (int i = 0; i < 4; i++)
                builder.append(" ");
        }else{
            fixed = true;
        }
        return this;
    }

    public ReplyBuilder newLine(){
        builder.append("\n");
        return this;
    }
    public ReplyBuilder nl(){
        return newLine();
    }

    public ReplyBuilder append(String appendObject){
        builder.append(appendObject);
        return this;
    }

    public ReplyBuilder append(Object appendObject){
        try{
            builder.append(appendObject);
        }catch(Exception e){
            System.err.println("Could not append object");
        }

        return this;
    }

    public String build(){
        return toString();
    }

    /**
     * Returns the String formatted properly based on the site
     * @return
     */
    public String toString(){
        if(!discord || !fixed)
            return builder.toString();

        String builderString = builder.toString();
        return "```\n" + builderString + "\n```";
    }
}
