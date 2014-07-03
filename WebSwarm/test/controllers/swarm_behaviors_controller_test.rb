require 'test_helper'

class SwarmBehaviorsControllerTest < ActionController::TestCase
  setup do
    @swarm_behavior = swarm_behaviors(:one)
  end

  test "should get index" do
    get :index
    assert_response :success
    assert_not_nil assigns(:swarm_behaviors)
  end

  test "should get new" do
    get :new
    assert_response :success
  end

  test "should create swarm_behavior" do
    assert_difference('SwarmBehavior.count') do
      post :create, swarm_behavior: { comparator_id: @swarm_behavior.comparator_id, depth_level: @swarm_behavior.depth_level, else_action_ids: @swarm_behavior.else_action_ids, else_number_bank: @swarm_behavior.else_number_bank, else_property_ids: @swarm_behavior.else_property_ids, if_action_ids: @swarm_behavior.if_action_ids, if_number_bank: @swarm_behavior.if_number_bank, if_property_ids: @swarm_behavior.if_property_ids, property_a_id: @swarm_behavior.property_a_id, property_b_id: @swarm_behavior.property_b_id, random_property_b: @swarm_behavior.random_property_b, subbehavior_ids: @swarm_behavior.subbehavior_ids }
    end

    assert_redirected_to swarm_behavior_path(assigns(:swarm_behavior))
  end

  test "should show swarm_behavior" do
    get :show, id: @swarm_behavior
    assert_response :success
  end

  test "should get edit" do
    get :edit, id: @swarm_behavior
    assert_response :success
  end

  test "should update swarm_behavior" do
    patch :update, id: @swarm_behavior, swarm_behavior: { comparator_id: @swarm_behavior.comparator_id, depth_level: @swarm_behavior.depth_level, else_action_ids: @swarm_behavior.else_action_ids, else_number_bank: @swarm_behavior.else_number_bank, else_property_ids: @swarm_behavior.else_property_ids, if_action_ids: @swarm_behavior.if_action_ids, if_number_bank: @swarm_behavior.if_number_bank, if_property_ids: @swarm_behavior.if_property_ids, property_a_id: @swarm_behavior.property_a_id, property_b_id: @swarm_behavior.property_b_id, random_property_b: @swarm_behavior.random_property_b, subbehavior_ids: @swarm_behavior.subbehavior_ids }
    assert_redirected_to swarm_behavior_path(assigns(:swarm_behavior))
  end

  test "should destroy swarm_behavior" do
    assert_difference('SwarmBehavior.count', -1) do
      delete :destroy, id: @swarm_behavior
    end

    assert_redirected_to swarm_behaviors_path
  end
end
