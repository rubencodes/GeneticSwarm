class AddNameToSwarmBehavior < ActiveRecord::Migration
  def change
		add_column :swarm_behaviors, :name, :string
  end
end
