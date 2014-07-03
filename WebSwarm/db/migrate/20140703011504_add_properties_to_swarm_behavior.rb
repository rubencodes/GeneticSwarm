class AddPropertiesToSwarmBehavior < ActiveRecord::Migration
  def change
    add_column :swarm_behaviors, :velocity_scale, :decimal
    add_column :swarm_behaviors, :max_speed, :decimal
    add_column :swarm_behaviors, :normal_speed, :decimal
    add_column :swarm_behaviors, :neighborhood_radius, :decimal
    add_column :swarm_behaviors, :separation_weight, :decimal
    add_column :swarm_behaviors, :cohesion_weight, :decimal
    add_column :swarm_behaviors, :pacekeeping_weight, :decimal
    add_column :swarm_behaviors, :rand_motion_probability, :decimal
  end
end
