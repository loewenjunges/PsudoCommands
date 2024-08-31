package me.zombie_striker.psudocommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class CommandUtils {

	public static final String EMPTY_COMMAND_ERROR = ChatColor.GRAY + "[PsudoCommands] Please provide a valid command.";

	private static final Pattern IS_DECIMAL = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)");

	public static boolean isSelector(String arg) {
		return Pattern.matches("@[aerps](\\[.*\\])?", arg);
	}

	public static boolean isSelectorWithoutTag(String arg) {
		return Pattern.matches("@[aerps](\\[ *\\])?", arg);
	}

	public static boolean isSelectorStartWithTag(String arg) {
		return Pattern.matches("@[aerps]\\[", arg);
	}

	// SuggestionProvider<CommandListenerWrapper>
	public static CompletableFuture<Suggestions> getArgumentSuggestion(CommandContext<?> context, SuggestionsBuilder builder, PsudoCommandExecutor executor, PluginCommand command) {
		CommandSender baseSender = PsudoReflection.getBukkitBasedSender(context.getSource());
		String[] args = builder.getRemaining().split(" ", -1); // -1 to keep trailing space
		List<String> completion = executor.onTabComplete(baseSender, command, null, args);
		if (completion != null) {
			// offset the builder to the next argument to not be stuck at the beginning
			int offset = 0;
			for (int i = 0; i < args.length - 1; i += 1) {
				offset += args[i].length() + 1; // +1 for the space deleted with split
			}
			builder = builder.createOffset(builder.getStart() + offset);

			// no need to check if the completion string starts with the remaining, already filter in onTabComplete
			for (String str : completion) {
				builder.suggest(str);
			}
		}
		return builder.buildFuture();
	}

	public static int getArgumentExecutes(CommandContext<?> context, PsudoCommandExecutor executor, PsudoCommandExecutor.PsudoCommandType commandType) {
		String[] args = StringArgumentType.getString(context, "psudoargs").split(" ");
		Object source = context.getSource();
		CommandSender baseSender = PsudoReflection.getBukkitBasedSender(source);
		CommandSender sender = PsudoReflection.getBukkitSender(source);
		if (sender == null) {
			sender = baseSender;
		}
		boolean result = executor.onCommand(baseSender, sender, source, commandType, args);
		return result ? 1 : 0;
	}

	// Every <Object> is actually a <CommandListenerWrapper>
	public static LiteralArgumentBuilder<Object> buildSpigotBrigadierCommand(PsudoCommandExecutor executor, PluginCommand command, PsudoCommandExecutor.PsudoCommandType commandType) {
		return LiteralArgumentBuilder.literal(command.getName())
				.executes(context -> {
					Object source = context.getSource();
					CommandSender baseSender = PsudoReflection.getBukkitBasedSender(source);
					baseSender.sendMessage(CommandUtils.EMPTY_COMMAND_ERROR);
					return 1;
				})
				.then(RequiredArgumentBuilder.argument("psudoargs", StringArgumentType.greedyString())
						.suggests((context, builder) -> getArgumentSuggestion(context, builder, executor, command))
						.executes(context -> getArgumentExecutes(context, executor, commandType))
				);
	}

	/**
	 * Modify the given String array to concat some arguments in one according
	 * to the beginning and the end. For example, if : begin and end are double
	 * quote " and sep is a space :
	 * split is ["first", "second", "\"combine", "args", "in", "one\"", "last"]
	 * index is 2
	 * -> returns ["first", "second", "combine args in one", "last"]
	 * If index isn't 2, nothing will happen.
	 * <p>
	 * Begin (resp. end) must be at the beginning (resp. the end) of their
	 * argument if they represent the beginning (resp. the end) of the new args.
	 * They are also removed.
	 * @param args The initial array.
	 * @param index The index of the start.
	 * @param begin String that represents the beginning of the concat argument.
	 * @param end String that represents the end of the concat argument.
	 * @param sep The separator, often a space.
	 * @return A new String array or the same reference as the given array.
	 */
	public static String[] combineArgs(String[] args, int index, String begin, String end, String sep) {
		if (index >= args.length) {
			return args;
		}
		String prevArg = args[index];
		if(prevArg.startsWith(begin)) {
			int beginLength = begin.length();
			int endLength = end.length();
			boolean onlyBegin = prevArg.equals(begin);
			if (onlyBegin && index == args.length-1) {
				// The index is the last and text only the beginning string... What's your problem ??
				args[index] = "";
				return args;
			} else if (!onlyBegin && prevArg.endsWith(end)) {
				// something like <"argument">, only remove quotes
				args[index] = prevArg.substring(beginLength, prevArg.length() - endLength);
				return args;
			} else {
				// Loop to the end or the arg with quotes in last char
				StringBuilder newArg = new StringBuilder(onlyBegin ? args[index+1] : prevArg.substring(beginLength));
				int i = index + (onlyBegin ? 2 : 1);
				while (i < args.length && !args[i].endsWith(end)) {
					newArg.append(sep).append(args[i]);
					i++;
				}

				// If true, it means it found the "end" string at the end of the current arg
				if (i < args.length) {
					newArg.append(sep).append(args[i], 0, args[i].length() - endLength);
					i++;
				}
				// modify this to copy
				args[index] = newArg.toString();

				// copy the split array into a new one.
				String[] newArgs = new String[args.length - i + index+1];
				System.arraycopy(args, 0, newArgs, 0, index+1);
				if (i < args.length) {
					System.arraycopy(args, i, newArgs, index + 1, args.length - i);
				}

				// set split like before copy
				args[index] = prevArg;

				return newArgs;
			}
		}
		return args;
	}

	/**
	 * Returns true if str is a relative, local or world coordinate.
	 * E.g. "^-2" or "~1.45" or "-2.4"
	 * If firstCoord is true, str has to be relative or local, with "~" or "^"
	 * to solve any ambiguity, easier to detect the first coordinate of the three.
	 * If it is false, str can be a double as string.
	 *
	 * @param str        The tested coordinate
	 * @param firstCoord Is str the first coordinate of the three.
	 * @return True if str is a relative coordinate
	 */
	public static boolean isRelativeCoord(String str, boolean firstCoord) {
		// return true if it is a used coordinated like ~3.4 or ^40 or 3.1 ...
		if(str.startsWith("~") || str.startsWith("^")) {
			return str.length() == 1 || isDouble(str.substring(1));
		}
		return !firstCoord && isDouble(str);
	}

	/**
	 * Parse string coordinates as double coordinates.
	 * Each string is a number, or starts with "~".
	 * Precondition : x, y and z verify isRelativeCoord.
	 *
	 * @param x      First coordinate
	 * @param y      Second coordinate
	 * @param z      Third coordinate
	 * @param origin The origin location for relative
	 * @return The arrival location
	 */
	public static Location getRelativeCoord(String x, String y, String z, Location origin) {
		Location arrival = origin.clone();
		arrival.setX(x.startsWith("~") ? arrival.getX() + getCoordinate(x) : Double.parseDouble(x));
		arrival.setY(y.startsWith("~") ? arrival.getY() + getCoordinate(y) : Double.parseDouble(y));
		arrival.setZ(z.startsWith("~") ? arrival.getZ() + getCoordinate(z) : Double.parseDouble(z));
		return arrival;
	}

	public static double getCoordinate(String c) {
		// c is like ^3 or ~-1.2 or 489.1
		if (Character.isDigit(c.charAt(0))) {
			return Double.parseDouble(c);
		} else {
			return c.length() == 1 ? 0 : Double.parseDouble(c.substring(1));
		}
	}

	private static boolean isDouble(String str) {
		return IS_DECIMAL.matcher(str).matches();
	}
}
