package com.feed_the_beast.ftbu.gui.guide;

import com.feed_the_beast.ftbl.lib.FinalIDObject;
import com.feed_the_beast.ftbl.lib.gui.GuiBase;
import com.feed_the_beast.ftbl.lib.gui.Widget;
import com.feed_the_beast.ftbl.lib.icon.Icon;
import com.feed_the_beast.ftbl.lib.internal.FTBLibLang;
import com.feed_the_beast.ftbl.lib.util.JsonUtils;
import com.feed_the_beast.ftbu.api.guide.IGuidePage;
import com.feed_the_beast.ftbu.api.guide.IGuideTextLine;
import com.feed_the_beast.ftbu.api.guide.IGuideTextLineProvider;
import com.feed_the_beast.ftbu.api.guide.SpecialGuideButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuidePage extends FinalIDObject implements IGuidePage
{
	public static final Map<String, IGuideTextLineProvider> LINE_PROVIDERS = new HashMap<>();

	public final List<IGuideTextLine> text;
	public final LinkedHashMap<String, IGuidePage> childPages;
	private IGuidePage parent;
	private ITextComponent title;
	private Icon pageIcon;
	private final List<SpecialGuideButton> specialButtons;

	public GuidePage(String id)
	{
		super(id);
		text = new ArrayList<>();
		childPages = new LinkedHashMap<>(0);
		pageIcon = Icon.EMPTY;
		specialButtons = new ArrayList<>();
	}

	public GuidePage(String id, @Nullable GuidePage p, JsonObject json)
	{
		this(id);
		parent = p;

		if (json.has("title"))
		{
			setTitle(JsonUtils.deserializeTextComponent(json.get("title")));
		}
		if (json.has("text"))
		{
			JsonArray a = json.get("text").getAsJsonArray();
			for (int i = 0; i < a.size(); i++)
			{
				text.add(createLine(a.get(i)));
			}
		}
		if (json.has("pages"))
		{
			JsonObject o1 = json.get("pages").getAsJsonObject();

			for (Map.Entry<String, JsonElement> entry : o1.entrySet())
			{
				childPages.put(entry.getKey(), new GuidePage(entry.getKey(), this, entry.getValue().getAsJsonObject()));
			}
		}
		if (json.has("icon"))
		{
			pageIcon = Icon.getIcon(json.get("icon"));
		}
		if (json.has("buttons"))
		{
			for (JsonElement e : json.get("buttons").getAsJsonArray())
			{
				specialButtons.add(new SpecialGuideButton(e.getAsJsonObject()));
			}
		}
	}

	@Override
	@Nullable
	public IGuidePage getParent()
	{
		return parent;
	}

	@Override
	public void setParent(IGuidePage page)
	{
		parent = page;
	}

	@Override
	public ITextComponent getDisplayName()
	{
		return (title == null) ? new TextComponentString(getName()) : title;
	}

	@Override
	@Nullable
	public ITextComponent getTitle()
	{
		return title;
	}

	@Override
	public IGuidePage setTitle(@Nullable ITextComponent t)
	{
		title = t;
		return this;
	}

	@Override
	public void println(@Nullable Object o)
	{
		if (o == null)
		{
			text.add(null);
		}
		else if (o instanceof IGuideTextLine)
		{
			if (o instanceof GuideTextLineString)
			{
				String text = ((GuideTextLineString) o).getUnformattedText();
				if (text.isEmpty())
				{
					println(null);
					return;
				}
				else if (text.startsWith("# "))
				{
					ITextComponent component = new TextComponentString(text.substring(2));
					component.getStyle().setBold(true);
					component.getStyle().setUnderlined(true);
					println(component);
					return;
				}
				else if (text.startsWith("## "))
				{
					ITextComponent component = new TextComponentString(text.substring(3));
					component.getStyle().setBold(true);
					println(component);
					return;
				}
			}

			text.add((IGuideTextLine) o);
		}
		else if (o instanceof ITextComponent)
		{
			ITextComponent c = (ITextComponent) o;

			if (c instanceof TextComponentString && c.getStyle().isEmpty() && c.getSiblings().isEmpty())
			{
				text.add(new GuideTextLineString(((TextComponentString) c).getText()));
			}
			else
			{
				text.add(new GuideExtendedTextLine(c));
			}
		}
		else if (o instanceof GuidePage)
		{
			copyFrom((GuidePage) o);
		}
		else
		{
			text.add(new GuideTextLineString(String.valueOf(o)));
		}
	}

	@Override
	public List<IGuideTextLine> getText()
	{
		return text;
	}

	@Override
	public Map<String, IGuidePage> getChildren()
	{
		return childPages;
	}

	@Override
	public IGuidePage getSub(String id)
	{
		IGuidePage p = childPages.get(id);

		if (p == null)
		{
			p = addSub(new GuidePage(id));
		}

		return p;
	}

	@Override
	public List<SpecialGuideButton> getSpecialButtons()
	{
		return specialButtons;
	}

	@Override
	public IGuidePage setIcon(Icon icon)
	{
		pageIcon = icon;
		return this;
	}

	@Override
	public Icon getIcon()
	{
		return pageIcon;
	}

	@Override
	public Widget createWidget(GuiBase gui)
	{
		return new ButtonGuidePage(gui, this, false);
	}

	@Override
	@Nullable
	public IGuideTextLine createLine(@Nullable JsonElement json)
	{
		if (json == null || json.isJsonNull())
		{
			return null;
		}
		else if (json.isJsonPrimitive())
		{
			String s = json.getAsString();
			return s.trim().isEmpty() ? null : new GuideTextLineString(s);
		}
		else if (json.isJsonArray())
		{
			return new GuideExtendedTextLine(json);
		}
		else
		{
			JsonObject o = json.getAsJsonObject();
			IGuideTextLineProvider provider = null;

			if (o.has("id"))
			{
				String id = o.get("id").getAsString();
				provider = LINE_PROVIDERS.get(id);

				if (provider == null)
				{
					ITextComponent component = FTBLibLang.ERROR.textComponent(id);
					component.getStyle().setColor(TextFormatting.DARK_RED);
					component.getStyle().setBold(true);
					return new GuideExtendedTextLine(component);
				}
			}
			/*
			else
            {
                provider = null;

                for(Map.Entry<String, JsonElement> entry : o.entrySet())
                {
                    provider = INFO_TEXT_LINE_PROVIDERS.get(entry.getKey());

                    if(provider != null)
                    {
                        break;
                    }
                }
            }*/

			IGuideTextLine line;

			if (provider != null)
			{
				line = provider.create(this, json);
			}
			else
			{
				line = new GuideExtendedTextLine(json);
			}

			return line;
		}
	}
}