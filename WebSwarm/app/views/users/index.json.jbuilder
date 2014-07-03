json.array!(@users) do |user|
  json.extract! user, :id, :username, :password, :school, :swarm_behaviors
  json.url user_url(user, format: :json)
end
