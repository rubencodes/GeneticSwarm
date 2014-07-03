class AddAlignmentWeightToSwarmBehavior < ActiveRecord::Migration
  def change
    add_column :swarm_behaviors, :alignment_weight, :decimal
  end
end
