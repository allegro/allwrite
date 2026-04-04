package pl.allegro.tech.allwrite.cli.application

internal abstract class ExternalSubCommand(
    name: String,
    help: String
) : SubCommand(name = name, help = help)
