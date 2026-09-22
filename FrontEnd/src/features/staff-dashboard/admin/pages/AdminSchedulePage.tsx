import AdminScheduleManager from '../components/AdminScheduleManager';
import PageLayout from '../../../../components/layout/PageLayout';

export default function AdminSchedulePage() {
  return (
    <PageLayout title="Schedule Management">
      <div className="max-w-2xl">
        <AdminScheduleManager />
      </div>
    </PageLayout>
  );
}
