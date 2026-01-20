import AdminLayout from '../../components/admin/AdminLayout'

export default function AdminPage() {
  return (
    <AdminLayout>
      <div className="w-full p-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">Admin 대시보드</h1>
          <p className="text-lg text-gray-600">관리자 페이지에 오신 것을 환영합니다.</p>
        </div>
        
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 w-full">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">시스템 현황</h2>
          <p className="text-base text-gray-600">대시보드 기능은 추후 구현 예정입니다.</p>
        </div>
      </div>
    </AdminLayout>
  )
}
