class User < ActiveRecord::Base
	has_many :swarm_behaviors
	before_create :generate_initial_behaviors
	
	def self.authenticate(email="", password="")
		user = User.find_by(username: email, password: password)
		if !user.nil?
			return user
		end
	end
	def generate_initial_behaviors
		12.times {
			self.swarm_behaviors << SwarmBehavior.create
		}
	end
	def start_over
		12.times {
			self.swarm_behaviors << SwarmBehavior.create
		}
	end
end
