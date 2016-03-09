package ftb.utils.mod.cmd.admin;

import ftb.lib.api.ForgePlayerMP;
import ftb.lib.api.cmd.*;
import ftb.utils.ranks.*;
import net.minecraft.command.*;
import net.minecraft.util.BlockPos;

import java.util.List;

/**
 * Created by LatvianModder on 21.02.2016.
 */
public class CmdSetRank extends CommandLM
{
	public CmdSetRank()
	{ super("setrank", CommandLevel.OP); }
	
	public boolean isUsernameIndex(String[] args, int i)
	{ return i == 0; }
	
	public List<String> addTabCompletionOptions(ICommandSender ics, String[] args, BlockPos pos)
	{
		if(args.length == 2) return getListOfStringsMatchingLastWord(args, Ranks.instance().ranks.keySet());
		return super.addTabCompletionOptions(ics, args, pos);
	}
	
	public void processCommand(ICommandSender ics, String[] args) throws CommandException
	{
		checkArgs(args, 2);
		ForgePlayerMP player = ForgePlayerMP.get(args[0]);
		Rank r = Ranks.instance().ranks.get(args[1]);
		if(r == null) throw new RawCommandException("Rank '" + args[1] + "' not found!");
		Ranks.instance().playerMap.put(player.getProfile().getId(), r);
		Ranks.instance().saveRanks();
	}
}
