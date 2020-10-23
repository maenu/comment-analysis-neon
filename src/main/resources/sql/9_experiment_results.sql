CREATE TABLE "{{data}}_9_experiment_results"
(
    "Key_Dataset"                       TEXT    NOT NULL,
    "Key_Run"                           INTEGER NOT NULL,
    "Key_Fold"                          INTEGER NOT NULL,
    "Key_Scheme"                        TEXT    NOT NULL,
    "Key_Scheme_options"                TEXT    NOT NULL,
    "Key_Scheme_version_ID"             INTEGER NOT NULL,
    "Date_time"                         NUMERIC NOT NULL,
    "Number_of_training_instances"      INTEGER NOT NULL,
    "Number_of_testing_instances"       INTEGER NOT NULL,
    "Number_correct"                    INTEGER NOT NULL,
    "Number_incorrect"                  INTEGER NOT NULL,
    "Number_unclassified"               INTEGER NOT NULL,
    "Percent_correct"                   NUMERIC NOT NULL,
    "Percent_incorrect"                 NUMERIC NOT NULL,
    "Percent_unclassified"              NUMERIC NOT NULL,
    "Kappa_statistic"                   NUMERIC NOT NULL,
    "Mean_absolute_error"               NUMERIC NOT NULL,
    "Root_mean_squared_error"           NUMERIC NOT NULL,
    "Relative_absolute_error"           NUMERIC NOT NULL,
    "Root_relative_squared_error"       NUMERIC NOT NULL,
    "SF_prior_entropy"                  NUMERIC NOT NULL,
    "SF_scheme_entropy"                 NUMERIC NOT NULL,
    "SF_entropy_gain"                   NUMERIC NOT NULL,
    "SF_mean_prior_entropy"             NUMERIC NOT NULL,
    "SF_mean_scheme_entropy"            NUMERIC NOT NULL,
    "SF_mean_entropy_gain"              NUMERIC NOT NULL,
    "KB_information"                    NUMERIC NOT NULL,
    "KB_mean_information"               NUMERIC NOT NULL,
    "KB_relative_information"           NUMERIC NOT NULL,
    "True_positive_rate"                NUMERIC,
    "Num_true_positives"                INTEGER NOT NULL,
    "False_positive_rate"               NUMERIC,
    "Num_false_positives"               INTEGER NOT NULL,
    "True_negative_rate"                NUMERIC,
    "Num_true_negatives"                INTEGER NOT NULL,
    "False_negative_rate"               NUMERIC,
    "Num_false_negatives"               INTEGER NOT NULL,
    "IR_precision"                      NUMERIC,
    "IR_recall"                         NUMERIC,
    "F_measure"                         NUMERIC,
    "Matthews_correlation"              NUMERIC,
    "Area_under_ROC"                    NUMERIC,
    "Area_under_PRC"                    NUMERIC,
    "Weighted_avg_true_positive_rate"   NUMERIC,
    "Weighted_avg_false_positive_rate"  NUMERIC,
    "Weighted_avg_true_negative_rate"   NUMERIC,
    "Weighted_avg_false_negative_rate"  NUMERIC,
    "Weighted_avg_IR_precision"         NUMERIC,
    "Weighted_avg_IR_recall"            NUMERIC,
    "Weighted_avg_F_measure"            NUMERIC,
    "Weighted_avg_matthews_correlation" NUMERIC,
    "Weighted_avg_area_under_ROC"       NUMERIC,
    "Weighted_avg_area_under_PRC"       NUMERIC,
    "Unweighted_macro_avg_F_measure"    NUMERIC,
    "Unweighted_micro_avg_F_measure"    NUMERIC,
    "Elapsed_Time_training"             NUMERIC NOT NULL,
    "Elapsed_Time_testing"              NUMERIC NOT NULL,
    "UserCPU_Time_training"             NUMERIC NOT NULL,
    "UserCPU_Time_testing"              NUMERIC NOT NULL,
    "UserCPU_Time_millis_training"      INTEGER NOT NULL,
    "UserCPU_Time_millis_testing"       INTEGER NOT NULL,
    "Serialized_Model_Size"             INTEGER NOT NULL,
    "Serialized_Train_Set_Size"         INTEGER NOT NULL,
    "Serialized_Test_Set_Size"          INTEGER NOT NULL,
    "Coverage_of_Test_Cases_By_Regions" NUMERIC NOT NULL,
    "Size_of_Predicted_Regions"         NUMERIC NOT NULL,
    "Summary"                           TEXT    NOT NULL,
    "features_tfidf"                    INTEGER NOT NULL,
    "features_heuristic"                INTEGER NOT NULL,
    "category"                          TEXT    NOT NULL
)
