package com.ftpserver.command;

import java.util.HashMap;
import java.util.Map;

public class CommandFactory {
    private static final Map<String, FtpCommand> COMMANDS = new HashMap<>();

    static {
        registerCommand("USER", new UserCommand());
        registerCommand("PASS", new PassCommand());
        registerCommand("QUIT", new QuitCommand());
        registerCommand("SYST", new SystCommand());
        registerCommand("FEAT", new FeatCommand());
        registerCommand("PWD", new PwdCommand());
        registerCommand("XPWD", new PwdCommand());
        registerCommand("CWD", new CwdCommand());
        registerCommand("XCWD", new CwdCommand());
        registerCommand("CDUP", new CdupCommand());
        registerCommand("XCUP", new CdupCommand());
        registerCommand("MKD", new MkdCommand());
        registerCommand("XMKD", new MkdCommand());
        registerCommand("RMD", new RmdCommand());
        registerCommand("XRMD", new RmdCommand());
        registerCommand("DELE", new DeleCommand());
        registerCommand("RNFR", new RnfrCommand());
        registerCommand("RNTO", new RntoCommand());
        registerCommand("LIST", new ListCommand());
        registerCommand("NLST", new NlstCommand());
        registerCommand("MLSD", new MlsdCommand());
        registerCommand("MLST", new MlstCommand());
        registerCommand("RETR", new RetrCommand());
        registerCommand("STOR", new StorCommand());
        registerCommand("TYPE", new TypeCommand());
        registerCommand("PORT", new PortCommand());
        registerCommand("EPRT", new EprtCommand());
        registerCommand("PASV", new PasvCommand());
        registerCommand("EPSV", new EpsvCommand());
        registerCommand("NOOP", new NoopCommand());
        registerCommand("OPTS", new OptsCommand());
        registerCommand("SITE", new SiteCommand());
        registerCommand("SIZE", new SizeCommand());
        registerCommand("MDTM", new MdtmCommand());
        registerCommand("REST", new RestCommand());
        registerCommand("ABOR", new AborCommand());
        registerCommand("HELP", new HelpCommand());
        registerCommand("STAT", new StatCommand());
    }

    private static void registerCommand(String name, FtpCommand command) {
        COMMANDS.put(name.toUpperCase(), command);
    }

    public static FtpCommand getCommand(String name) {
        return COMMANDS.get(name.toUpperCase());
    }

    public static boolean isCommandSupported(String name) {
        return COMMANDS.containsKey(name.toUpperCase());
    }
}
