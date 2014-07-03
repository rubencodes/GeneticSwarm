require 'test_helper'

class AddRatingToSwarmBehaviorsControllerTest < ActionController::TestCase
  setup do
    @add_rating_to_swarm_behavior = add_rating_to_swarm_behaviors(:one)
  end

  test "should get index" do
    get :index
    assert_response :success
    assert_not_nil assigns(:add_rating_to_swarm_behaviors)
  end

  test "should get new" do
    get :new
    assert_response :success
  end

  test "should create add_rating_to_swarm_behavior" do
    assert_difference('AddRatingToSwarmBehavior.count') do
      post :create, add_rating_to_swarm_behavior: { rating: @add_rating_to_swarm_behavior.rating }
    end

    assert_redirected_to add_rating_to_swarm_behavior_path(assigns(:add_rating_to_swarm_behavior))
  end

  test "should show add_rating_to_swarm_behavior" do
    get :show, id: @add_rating_to_swarm_behavior
    assert_response :success
  end

  test "should get edit" do
    get :edit, id: @add_rating_to_swarm_behavior
    assert_response :success
  end

  test "should update add_rating_to_swarm_behavior" do
    patch :update, id: @add_rating_to_swarm_behavior, add_rating_to_swarm_behavior: { rating: @add_rating_to_swarm_behavior.rating }
    assert_redirected_to add_rating_to_swarm_behavior_path(assigns(:add_rating_to_swarm_behavior))
  end

  test "should destroy add_rating_to_swarm_behavior" do
    assert_difference('AddRatingToSwarmBehavior.count', -1) do
      delete :destroy, id: @add_rating_to_swarm_behavior
    end

    assert_redirected_to add_rating_to_swarm_behaviors_path
  end
end
