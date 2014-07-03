class AddRatingToSwarmBehavior < ActiveRecord::Migration
  def change
    add_column :swarm_behaviors, :rating, :integer
  end
end
