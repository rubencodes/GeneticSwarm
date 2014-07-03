class User < ActiveRecord::Base
	has_many :swarm_behaviors
	before_create :generate_initial_behaviors
	
	def generate_initial_behaviors
		10.times {
			self.swarm_behaviors << SwarmBehavior.create
		}
	end
end
