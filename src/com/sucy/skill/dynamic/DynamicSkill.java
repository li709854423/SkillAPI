package com.sucy.skill.dynamic;

import com.sucy.skill.api.event.PhysicalDamageEvent;
import com.sucy.skill.api.event.SkillDamageEvent;
import com.sucy.skill.api.skills.PassiveSkill;
import com.sucy.skill.api.skills.Skill;
import com.sucy.skill.api.skills.SkillShot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * A skill implementation for the Dynamic system
 */
public class DynamicSkill extends Skill implements SkillShot, PassiveSkill, Listener
{
    private HashMap<Trigger, EffectComponent> components = new HashMap<Trigger, EffectComponent>();
    private HashMap<UUID, Integer>            active     = new HashMap<UUID, Integer>();

    /**
     * Initializes a new dynamic skill
     *
     * @param name name of the skill
     */
    public DynamicSkill(String name)
    {
        super(name, "Dynamic", Material.JACK_O_LANTERN, 1);
    }

    // Testing method
    public void addComponent(Trigger trigger, EffectComponent component)
    {
        components.put(trigger, component);
    }

    /**
     * Checks whether or not the dynamic skill can be cast
     *
     * @return true if can cast, false otherwise
     */
    public boolean canCast()
    {
        return components.containsKey(Trigger.CAST);
    }

    /**
     * Updates the skill effects
     *
     * @param user      user to refresh the effect for
     * @param prevLevel previous skill level
     * @param newLevel  new skill level
     */
    @Override
    public void update(LivingEntity user, int prevLevel, int newLevel)
    {
        active.put(user.getUniqueId(), newLevel);
    }

    /**
     * Initializes any corresponding effects when the skill is unlocked/enabled
     *
     * @param user  user to initialize the effects for
     * @param level skill level
     */
    @Override
    public void initialize(LivingEntity user, int level)
    {
        trigger(user, user, level, Trigger.INITIALIZE);
        active.put(user.getUniqueId(), level);
    }

    /**
     * Removes active users from the map
     *
     * @param user  user to stop the effects for
     * @param level skill level
     */
    @Override
    public void stopEffects(LivingEntity user, int level)
    {
        active.remove(user.getUniqueId());
    }

    /**
     * Casts the skill if applicable
     *
     * @param user  user of the skill
     * @param level skill level
     *
     * @return true if casted successfully, false if conditions weren't met or no effects are using the cast trigger
     */
    @Override
    public boolean cast(LivingEntity user, int level)
    {
        return trigger(user, user, level, Trigger.CAST);
    }

    /**
     * Applies the death trigger effects
     *
     * @param event event details
     */
    @EventHandler
    public void onDeath(EntityDeathEvent event)
    {
        if (active.containsKey(event.getEntity().getUniqueId()))
        {
            trigger(event.getEntity(), event.getEntity(), active.get(event.getEntity().getUniqueId()), Trigger.DEATH);
        }
    }

    /**
     * Applies physical damage triggers
     *
     * @param event event details
     */
    @EventHandler
    public void onPhysical(PhysicalDamageEvent event)
    {
        LivingEntity damager = event.getDamager();
        LivingEntity target = event.getTarget();
        boolean projectile = event.isProjectile();

        // Physical receieved
        EffectComponent component = components.get(Trigger.TOOK_PHYSICAL_DAMAGE);
        if (component != null && active.containsKey(target.getUniqueId()))
        {
            String type = component.settings.getString("type", "both").toLowerCase();
            double min = component.settings.get("dmg-min");
            double max = component.settings.get("dmg-max");

            if (event.getDamage() >= min && event.getDamage() <= max
                    && (type.equals("both") || type.equals("projectile") == projectile))
            {
                trigger(target, damager, active.get(event.getTarget().getUniqueId()), Trigger.TOOK_PHYSICAL_DAMAGE);
            }
        }

        // Physical dealt
        component = components.get(Trigger.PHYSICAL_DAMAGE);
        if (component != null && active.containsKey(damager.getUniqueId()))
        {
            String type = component.settings.getString("type", "both").toLowerCase();
            double min = component.settings.get("dmg-min");
            double max = component.settings.get("dmg-max");

            if (event.getDamage() >= min && event.getDamage() <= max
                    && (type.equals("both") || type.equals("projectile") == projectile))
            {
                trigger(damager, target, active.get(event.getTarget().getUniqueId()), Trigger.PHYSICAL_DAMAGE);
            }
        }
    }

    /**
     * Applies skill damage triggers
     *
     * @param event event details
     */
    @EventHandler
    public void onSkillDamage(SkillDamageEvent event)
    {
        LivingEntity damager = event.getCaster();
        LivingEntity target = event.getTarget();

        // Skill received
        EffectComponent component = components.get(Trigger.TOOK_SKILL_DAMAGE);
        if (component != null && active.containsKey(target.getUniqueId()))
        {
            double min = component.settings.get("dmg-min");
            double max = component.settings.get("dmg-max");

            if (event.getDamage() >= min && event.getDamage() <= max)
            {
                trigger(target, damager, active.get(event.getTarget().getUniqueId()), Trigger.TOOK_SKILL_DAMAGE);
            }
        }

        // Skill dealt
        component = components.get(Trigger.SKILL_DAMAGE);
        if (component != null && active.containsKey(damager.getUniqueId()))
        {
            double min = component.settings.get("dmg-min");
            double max = component.settings.get("dmg-max");

            if (event.getDamage() >= min && event.getDamage() <= max)
            {
                trigger(damager, target, active.get(event.getTarget().getUniqueId()), Trigger.TOOK_SKILL_DAMAGE);
            }
        }
    }

    /**
     * Applies crouch triggers
     *
     * @param event event details
     */
    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent event)
    {
        EffectComponent component = components.get(Trigger.CROUCH);
        if (component != null && active.containsKey(event.getPlayer().getUniqueId()))
        {
            if (event.isSneaking() != component.settings.getString("type", "start crouching").toLowerCase().equals("stop crouching"))
            {
                trigger(event.getPlayer(), event.getPlayer(), active.get(event.getPlayer().getUniqueId()), Trigger.CROUCH);
            }
        }
    }

    private boolean trigger(LivingEntity user, LivingEntity target, int level, Trigger trigger)
    {
        if (user != null && components.containsKey(trigger))
        {
            ArrayList<LivingEntity> self = new ArrayList<LivingEntity>();
            self.add(user);
            return components.get(trigger).execute(user, level, self);
        }
        return false;
    }

    /**
     * Loads dynamic components in addition to the normal values
     *
     * @param config config data to load from
     */
    @Override
    public void load(ConfigurationSection config)
    {
        ConfigurationSection triggers = config.getConfigurationSection("components");
        for (String key : triggers.getKeys(false))
        {
            try
            {
                Trigger trigger = Trigger.valueOf(key.toUpperCase().replace(' ', '_'));
                EffectComponent component = trigger.getComponent();
                component.load(this, triggers.getConfigurationSection(key));
                components.put(trigger, component);
            }
            catch (Exception ex)
            {
                // Invalid trigger
                Bukkit.getLogger().warning("Invalid trigger for the skill \"" + getName() + "\" - \"" + key + "\"");
            }
        }

        super.load(config);
    }
}
