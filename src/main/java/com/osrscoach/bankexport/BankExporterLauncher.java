package com.osrscoach.bankexport;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankExporterLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(
                BankExporterPlugin.class
        );

        RuneLite.main(args);
    }
}