package pl.allegro.tech.allwrite.recipes

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import pl.allegro.tech.allwrite.runtime.RuntimeModule

@Module(includes = [RuntimeModule::class])
@ComponentScan
public class RecipesModule
