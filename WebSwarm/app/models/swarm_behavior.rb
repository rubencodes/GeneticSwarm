class SwarmBehavior < ActiveRecord::Base
	before_create :generate_new
	
	def generate_new
		self.comparator_id = rand(3)	#0,1,2
		self.property_a_id = rand(9)	#0,1,2...8
		self.property_b_id = rand(9)	#0,1,2...8
		self.random_property_b 	= rand(0).zero? #0:true, 1:false
		self.depth_level	 			= self.depth_level || 0 #0 unless set
		
		if_property_id_array		= rand(9).times.map{ rand(9) }	#array size 0-8 of numbers 0-8
		else_property_id_array	= rand(9).times.map{ rand(9) }	#array size 0-8 of numbers 0-8
		self.if_property_ids		= if_property_id_array.join(",")
		self.else_property_ids	= else_property_id_array.join(",")
		self.if_action_ids			= if_property_id_array.length.times.map 	{ rand(3) }.join(",")
		self.else_action_ids		= else_property_id_array.length.times.map { rand(3) }.join(",")
		if_number_bank_array 		= []
		if_property_id_array.length.times do |i|
			if_number_bank_array.push(random_in_range(if_property_id_array[i]))
		end
		self.if_number_bank			= if_number_bank_array.join(",")
		
		else_number_bank_array 	= []
		else_property_id_array.length.times do |i|
			else_number_bank_array.push(random_in_range(else_property_id_array[i]))
		end
		self.else_number_bank		= else_number_bank_array.join(",")
		
		#behavior properties
		self.velocity_scale						= random_in_range 0
		self.max_speed								= random_in_range 1
		self.normal_speed							= random_in_range 2
		self.neighborhood_radius			= random_in_range 3
		self.separation_weight				= random_in_range 4
		self.alignment_weight					= random_in_range 5
		self.cohesion_weight					= random_in_range 6
		self.pacekeeping_weight				= random_in_range 7
		self.rand_motion_probability	= random_in_range 8
		self.rating										= 0
		
		if rand(2).zero?
			@sub = SwarmBehavior.create(depth_level: self.depth_level+1)
			self.subbehavior_ids = @sub.id
		end
	end
	
	def random_in_range(property_id)
		case property_id
			when 0
				return rand / 10.0
			when 1
				return rand(9) + 2
			when 2
				return rand(9) + 2
			when 3
				return rand(91) + 10
			when 4
				return rand(101)
			when 5
				return rand
			when 6
				return rand
			when 7
				return rand
			when 8
				return rand / 2
		end
	end
	
	def self.get_next_swarm_behavior(user_id)
		@swarm_behaviors 	= User.find_by(id: user_id).swarm_behaviors
		@swarm_behaviors.where(rating: 0).order(:created_at).first
	end
end
