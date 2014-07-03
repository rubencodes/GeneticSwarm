class CreateAddRatingToSwarmBehaviors < ActiveRecord::Migration
  def change
    create_table :add_rating_to_swarm_behaviors do |t|
      t.integer :rating

      t.timestamps
    end
  end
end
