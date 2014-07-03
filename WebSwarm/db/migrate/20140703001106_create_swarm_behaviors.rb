class CreateSwarmBehaviors < ActiveRecord::Migration
  def change
    create_table :swarm_behaviors do |t|
      t.belongs_to :user
      t.integer :comparator_id
      t.integer :property_a_id
      t.integer :property_b_id
      t.boolean :random_property_b
      t.integer :depth_level
      t.string :if_property_ids
      t.string :if_action_ids
      t.string :else_property_ids
      t.string :else_action_ids
      t.string :if_number_bank
      t.string :else_number_bank
      t.string :subbehavior_ids
      t.timestamps
    end
  end
end
