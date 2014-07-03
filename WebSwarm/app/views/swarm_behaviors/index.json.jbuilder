json.array!(@swarm_behaviors) do |swarm_behavior|
  json.extract! swarm_behavior, :id, :comparator_id, :property_a_id, :property_b_id, :random_property_b, :depth_level, :if_property_ids, :if_action_ids, :else_property_ids, :else_action_ids, :if_number_bank, :else_number_bank, :subbehavior_ids, :velocity_scale, :max_speed, :normal_speed, :neighborhood_radius, :separation_weight, :alignment_weight, :cohesion_weight, :pacekeeping_weight, :rand_motion_probability, :rating
  json.url swarm_behavior_url(swarm_behavior, format: :json)
end
